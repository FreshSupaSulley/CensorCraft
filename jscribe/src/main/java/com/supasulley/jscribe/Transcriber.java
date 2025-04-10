package com.supasulley.jscribe;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

import javax.sound.sampled.LineUnavailableException;

import io.github.givimad.whisperjni.WhisperContext;
import io.github.givimad.whisperjni.WhisperFullParams;
import io.github.givimad.whisperjni.WhisperJNI;
import net.lingala.zip4j.ZipFile;

/**
 * Transcriber waits for new audio samples and processes them into text segments using {@linkplain WhisperJNI}.
 */
public class Transcriber extends Thread implements Runnable {
	
	private final WhisperJNI whisper = new WhisperJNI();
	private final LinkedBlockingQueue<float[]> samples = new LinkedBlockingQueue<float[]>();
	private final StringBuffer buffer = new StringBuffer();
	
	private static final WhisperFullParams params;
	
	private Path modelPath;
	private boolean running = true;
	
	private long timeToTranscribe;
	
	static
	{
		// These params stay the same for each request
		params = new WhisperFullParams();
		params.singleSegment = true;
		params.printProgress = false;
		params.printTimestamps = false;
		params.suppressNonSpeechTokens = true;
		params.suppressBlank = true;
	}
	
	public static void loadNatives() throws IOException
	{
		// Point to directory
		// System.setProperty("io.github.givimad.whisperjni.libdir", new File("lib/macos-amd64").getAbsolutePath());
		String osName = System.getProperty("os.name").toLowerCase();
		String osArch = System.getProperty("os.arch").toLowerCase();
		
		JScribe.logger.info("OS: " + osName);
		JScribe.logger.info("Arch: " + osArch);
		
		String resourceName = null;
		
		// Mac
		if(osName.contains("mac") || osName.contains("darwin"))
		{
			JScribe.logger.info("On Mac");
			
			// osArch doesn't help for differentiating x86-64 / Arm macs
			// String trueArch = new String(new ProcessBuilder("uname", "-m").start().getInputStream().readAllBytes()).trim();
			// JScribe.logger.info("True arch: {}", trueArch);
			
			String trueArch = osArch;
			
			if(trueArch.contains("x86_64"))
			{
				resourceName = "macos-amd64";
			}
			else if(trueArch.contains("aarch64") || trueArch.contains("arm64"))
			{
				resourceName = "macos-arm64";
			}
		}
		else if(osName.contains("win"))
		{
			JScribe.logger.info("On Windows");
			
			if(osArch.contains("amd64") || osArch.contains("x86_64"))
			{
				resourceName = "win-amd64";
			}
		}
		else if(osName.contains("nix") || osName.contains("nux") || osName.contains("aix"))
		{
			JScribe.logger.info("On Linux");
			
			if(osArch.contains("amd64") || osArch.contains("x86_64"))
			{
				resourceName = "debian-amd64";
			}
			else if(osArch.contains("aarch64") || osArch.contains("arm64"))
			{
				resourceName = "debian-arm64";
			}
			else if(osArch.contains("armv7") || osArch.contains("arm"))
			{
				resourceName = "debian-armv7l";
			}
		}
		
		if(resourceName == null)
		{
			throw new IllegalStateException("Native libraries not available for this OS: " + osName + ", Arch: " + osArch);
		}
		
		JScribe.logger.info("Loading libraries for " + resourceName);
		
		Stream.of(extractZipToTemp("lib/" + resourceName).listFiles()).forEach(file ->
		{
			JScribe.logger.info("Loading library at " + file);
			System.load(file.getAbsolutePath());
			file.deleteOnExit();
		});
		
		WhisperJNI.setLibraryLogger(null);
	}
	
	private static File extractZipToTemp(String zipName) throws IOException
	{
		Path tempZip = Files.createTempFile("temp", ".zip");
		Files.copy(Transcriber.class.getClassLoader().getResourceAsStream(zipName + ".zip"), tempZip, StandardCopyOption.REPLACE_EXISTING);
		
		try(ZipFile zip = new ZipFile(tempZip.toFile()))
		{
			Path destination = Files.createTempDirectory("temp");
			destination.toFile().deleteOnExit();
			
			// Extract it at the same destination
			zip.extractAll(destination.toString());
			
			// Pass in child name so we're not giving the parent folder back
			return destination.toFile();
		} finally
		{
			// We can delete the copied, unzipped zip
			tempZip.toFile().delete();
		}
	}
	
	public Transcriber(Path modelPath)
	{
		this.modelPath = modelPath;
		setName("JScribe Transcriber");
		setDaemon(true);
	}
	
	@Override
	public void run()
	{
		try(WhisperContext ctx = whisper.init(modelPath))
		{
			while(running)
			{
				if(samples.size() == 0)
				{
					continue;
				}
				
				long startTime = System.currentTimeMillis();
				
				// Squash all requests into one batch to stay up to date
				final int numSamples = samples.size();
				float[][] collectSamples = new float[numSamples][];
				
				int bufferSize = 0;
				
				for(int i = 0; i < numSamples; i++)
				{
					collectSamples[i] = samples.take();
					bufferSize += collectSamples[i].length;
				}
				
				// Now merge into one
				float[] toProcess = new float[bufferSize];
				
				for(int bufferIndex = 0, i = 0; i < numSamples; i++)
				{
					System.arraycopy(collectSamples[i], 0, toProcess, bufferIndex, collectSamples[i].length);
					bufferIndex += collectSamples[i].length;
				}
				
				final float[] samples2 = toProcess;
				Thread thread = new Thread(() ->
				{
					try
					{
						AudioRecorder.writeWavFile(System.currentTimeMillis() + ".wav", samples2, AudioRecorder.FORMAT.getSampleRate(), AudioRecorder.FORMAT.getChannels());
					} catch(IOException | LineUnavailableException e)
					{
						// FIXME Auto-generated catch block
						e.printStackTrace();
					}
				});
				thread.start();
				
				JScribe.logger.info("Transcribing {} recordings", numSamples);
				
				// Pass samples to whisper
				int result = whisper.full(ctx, params, toProcess, toProcess.length);
				
				if(result != 0)
				{
					JScribe.logger.error("Whisper failed with code {}", result);
					continue;
				}
				
				int numSegments = whisper.fullNSegments(ctx);
				
				for(int i = 0; i < numSegments; i++)
				{
					String text = whisper.fullGetSegmentText(ctx, i).trim();
					// can suppress this in whisper context
					if(text.equals("."))
						continue;
					// if(text.equals("[BLANK_AUDIO]"))
					// continue;
					
					JScribe.logger.trace("Transcription: {}", text);
					buffer.append(text);
				}
				
				timeToTranscribe = System.currentTimeMillis() - startTime;
				JScribe.logger.trace("Took {}ms to transcribe", timeToTranscribe);
			}
		} catch(IOException e)
		{
			JScribe.logger.error("Failed to init whisper", e);
		} catch(InterruptedException e)
		{
			JScribe.logger.trace("Interrupted waiting for new sample", e);
		} finally
		{
			// Ensure running is set to false
			shutdown();
		}
	}
	
	public void shutdown()
	{
		samples.clear();
		running = false;
		interrupt();
	}
	
	public int getBacklog()
	{
		return samples.size();
	}
	
	public long getLastTranscriptionTime()
	{
		return timeToTranscribe;
	}
	
	/**
	 * Gets all transcribed words and clears the buffer.
	 * 
	 * @return buffer of transcribed words
	 */
	public String getBuffer()
	{
		String result = buffer.toString();
		buffer.setLength(0);
		return result;
	}
	
	public void newSample(float[] sample)
	{
		if(!running)
		{
			throw new IllegalStateException("Transcriber is dead");
		}
		
		samples.add(sample);
	}
}
