package com.supasulley.jscribe;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

import io.github.givimad.whisperjni.WhisperContext;
import io.github.givimad.whisperjni.WhisperFullParams;
import io.github.givimad.whisperjni.WhisperJNI;
import net.lingala.zip4j.ZipFile;

/**
 * Transcriber waits for new audio samples and processes them into text segments using {@linkplain WhisperJNI}.
 */
public class Transcriber extends Thread implements Runnable {
	
	private final WhisperJNI whisper = new WhisperJNI();
	private final LinkedBlockingQueue<AudioSample> samples = new LinkedBlockingQueue<AudioSample>();
	private final StringBuffer buffer = new StringBuffer();
	
	private static final WhisperFullParams params;
	
	private Path modelPath;
	private boolean running = true;
	
	private long lastTimestamp = System.currentTimeMillis();
	
	static
	{
		// These params stay the same for each request
		params = new WhisperFullParams();
		params.singleSegment = true;
		params.printProgress = false;
		params.printTimestamps = false;
		params.suppressNonSpeechTokens = true;
		params.suppressBlank = true;
		
		try
		{
			// Load natives
			Transcriber.loadNatives();
		} catch(IOException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void loadNatives() throws IOException
	{
		// Point to directory
		// System.setProperty("io.github.givimad.whisperjni.libdir", new File("lib/macos-amd64").getAbsolutePath());
		String osName = System.getProperty("os.name").toLowerCase();
		String osArch = System.getProperty("os.arch").toLowerCase();
		
		JScribe.logger.debug("OS: " + osName);
		JScribe.logger.debug("Arch: " + osArch);
		
		String resourceName = null;
		
		// Mac
		if(osName.contains("mac") || osName.contains("darwin"))
		{
			JScribe.logger.debug("On Mac");
			
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
			JScribe.logger.debug("On Windows");
			
			if(osArch.contains("amd64") || osArch.contains("x86_64"))
			{
				resourceName = "win-amd64";
			}
		}
		else if(osName.contains("nix") || osName.contains("nux") || osName.contains("aix"))
		{
			JScribe.logger.debug("On Linux");
			
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
		
		Stream.of(extractZipToTemp("natives/" + resourceName).listFiles()).forEach(file ->
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
				
				// Squash all requests into one batch to stay up to date
				final int numSamples = samples.size();
				float[][] collectSamples = new float[numSamples][];
				long firstTimestamp = 0;
				int bufferSize = 0;
				
				for(int i = 0; i < numSamples; i++)
				{
					AudioSample sample = samples.take();
					collectSamples[i] = sample.samples();
					
					// The first timestamp is all we care about
					if(firstTimestamp == 0)
					{
						firstTimestamp = sample.timestamp();
					}
					
					bufferSize += collectSamples[i].length;
				}
				
				// Now merge into one
				float[] toProcess = new float[bufferSize];
				
				for(int bufferIndex = 0, i = 0; i < numSamples; i++)
				{
					System.arraycopy(collectSamples[i], 0, toProcess, bufferIndex, collectSamples[i].length);
					bufferIndex += collectSamples[i].length;
				}
				
				// final float[] samples2 = toProcess;
				// Thread thread = new Thread(() ->
				// {
				// try
				// {
				// AudioRecorder.writeWavFile(System.currentTimeMillis() + ".wav", samples2, AudioRecorder.FORMAT.getSampleRate(), AudioRecorder.FORMAT.getChannels());
				// } catch(IOException | LineUnavailableException e)
				// {
				// // FIXME Auto-generated catch block
				// e.printStackTrace();
				// }
				// });
				// thread.start();
				
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
				
				lastTimestamp = firstTimestamp;
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
		// Stop runner from recording audio
		interrupt();
		samples.clear();
		running = false;
	}
	
	public long getTimeBehind()
	{
		return System.currentTimeMillis() - lastTimestamp;
	}
	
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
		
		samples.add(new AudioSample(System.currentTimeMillis(), sample));
	}
	
	private record AudioSample(long timestamp, float[] samples) {
	}
}
