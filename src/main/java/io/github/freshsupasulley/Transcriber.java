package io.github.freshsupasulley;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.LinkedBlockingQueue;

import de.maxhenkel.rnnoise4j.Denoiser;
import de.maxhenkel.rnnoise4j.UnknownPlatformException;
import io.github.givimad.whisperjni.WhisperContext;
import io.github.givimad.whisperjni.WhisperFullParams;
import io.github.givimad.whisperjni.WhisperJNI;

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
		
		Transcriber.loadNatives();
	}
	
	public static void loadNatives()
	{
		JScribe.logger.info("Loading libraries");
		
		try
		{
			LibraryLoader.loadBundledNatives((a, b) ->
			{
				// Load JNI shit last if possible because it depends on whisper being loaded first
				boolean aJNI = a.getName().toLowerCase().contains("jni");
				boolean bJNI = b.getName().toLowerCase().contains("jni");
				return Boolean.compare(aJNI, bJNI);
			});
			
			// Then test loading whisper
			
			try
			{
				Denoiser denoiser = new Denoiser();
			} catch(UnknownPlatformException e)
			{
				throw new IOException(e);
			}
			
			WhisperJNI.setLibraryLogger(null);
		} catch(IOException | UnsatisfiedLinkError e)
		{
			JScribe.logger.error("An error occurred loading natives (platform: {}, arch: {})", LibraryLoader.OS_NAME, LibraryLoader.OS_ARCH, e);
			System.exit(1);
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
				
				JScribe.logger.debug("Transcribing {} recordings", numSamples);
				
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
					
					JScribe.logger.debug("Transcription: {}", text);
					buffer.append(text);
				}
				
				lastTimestamp = firstTimestamp;
			}
		} catch(IOException e)
		{
			JScribe.logger.error("Failed to init whisper", e);
		} catch(InterruptedException e)
		{
			JScribe.logger.debug("Interrupted waiting for new sample", e);
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
