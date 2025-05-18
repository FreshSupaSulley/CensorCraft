package io.github.freshsupasulley;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.freshsupasulley.Transcriptions.Transcription;
import io.github.givimad.whisperjni.WhisperContext;
import io.github.givimad.whisperjni.WhisperFullParams;
import io.github.givimad.whisperjni.WhisperJNI;
import io.github.givimad.whisperjni.WhisperState;
import io.github.givimad.whisperjni.internal.LibraryUtils;

/**
 * Transcriber waits for new audio samples and processes them into text segments using {@linkplain WhisperJNI}.
 */
class Transcriber extends Thread implements Runnable {
	
	// Maximum length a single transcription request can be
	private static final int MAX_LENGTH_MS = 30000;
	
	private final WhisperJNI whisper = new WhisperJNI();
	private final LinkedBlockingQueue<Recording> recordings = new LinkedBlockingQueue<Recording>();
	private final StringBuffer buffer = new StringBuffer();
	private final List<Transcription> results = new ArrayList<Transcription>();
	
	private static final WhisperFullParams params;
	
	private Path modelPath;
	private boolean running = true;
	private AtomicBoolean abandonSample = new AtomicBoolean();
	
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
		
		// Transcriber.loadWhisperJNI();
	}
	
	private static void loadWhisperJNI() throws IOException
	{
		JScribe.logger.info("Loading whisper-jni");
		// Not dealing with this unless I wanna fork it (hell naww)
		// System.setProperty("io.github.givimad.whisperjni.libdir", LibraryLoader.extractToTemp().toString());
		
		// path needs to be valid on windows, opened a pr :(
		LibraryUtils.loadLibrary(JScribe.logger::debug);
		// Then test loading whisper
		WhisperJNI.setLibraryLogger(JScribe.logger::info);
	}
	
	public Transcriber(Path modelPath, boolean useVulkan)
	{
		this.modelPath = modelPath;
		setName("JScribe Transcriber");
		setDaemon(true);
		
		try
		{
			if(!useVulkan)
			{
				Transcriber.loadWhisperJNI();
			}
			else
			{
				if(!LibraryLoader.canUseVulkan())
					throw new IOException("This system can't load Vulkan natives");
				
				JScribe.logger.info("Loading Vulkan natives");
				
				Path tempDir = LibraryLoader.extractFolderToTemp("win-amd64-vulkan");
				System.load(tempDir.resolve("ggml.dll").toAbsolutePath().toString());
				System.load(tempDir.resolve("whisper.dll").toAbsolutePath().toString());
				System.load(tempDir.resolve("whisper-jni.dll").toAbsolutePath().toString());
			}
		} catch(IOException | UnsatisfiedLinkError e)
		{
			JScribe.logger.error("An error occurred loading natives (platform: {}, arch: {})", System.getProperty("os.name"), System.getProperty("os.arch"), e);
			System.exit(1);
		}
	}
	
	@Override
	public void run()
	{
		try(WhisperContext ctx = whisper.initNoState(modelPath); WhisperState state = whisper.initState(ctx))
		{
			while(running)
			{
				abandonSample.set(false);
				
				if(recordings.size() == 0)
				{
					lastTimestamp = System.currentTimeMillis();
					continue;
				}
				
				float[] cumulative = new float[(int) (AudioRecorder.FORMAT.getSampleRate() * (MAX_LENGTH_MS / 1000f))];
				
				int sampleIndex = 0;
				int numRecordings = 0;
				long firstTimestamp = 0;
				
				List<Runnable> runnables = new ArrayList<Runnable>();
				
				// Keep harvesting until there's no more recordings in the queue
				for(Recording sample = null; (sample = recordings.poll()) != null; numRecordings++)
				{
					if(numRecordings == 0)
					{
						firstTimestamp = sample.timeReceived();
					}
					
					int sampleLength = sample.samples().length;
					
					if(sampleIndex + sampleLength > cumulative.length)
					{
						JScribe.logger.warn("JScribe can't keep up! Tried to transcribe audio longer than {}ms", MAX_LENGTH_MS);
						break; // abandon the next samples who cares
					}
					
					// Copy samples from this recording into cumulative window
					System.arraycopy(sample.samples(), 0, cumulative, sampleIndex, sampleLength);
					sampleIndex += sampleLength;
					
					// Also keep track of any completables
					if(sample.callback() != null)
					{
						runnables.add(sample.callback());
					}
				}
				
				if(numRecordings == 0)
				{
					continue;
				}
				
				// Now merge into one
				float[] toProcess = new float[sampleIndex];
				System.arraycopy(cumulative, 0, toProcess, 0, sampleIndex);
				
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
				
				JScribe.logger.debug("Transcribing {} recordings (length {})", numRecordings, toProcess.length);
				
				// Pass samples to whisper
				int result = whisper.fullWithState(ctx, state, params, toProcess, toProcess.length);
				
				if(result != 0)
				{
					JScribe.logger.error("Whisper failed with code {}", result);
					continue;
				}
				
				int numSegments = whisper.fullNSegmentsFromState(state);
				
				// does it need to be atomic?
				if(!abandonSample.get())
				{
					for(int i = 0; i < numSegments; i++)
					{
						String text = whisper.fullGetSegmentTextFromState(state, i).trim();
						// can suppress this in whisper context
						// if(text.equals("."))
						// continue;
						// if(text.equals("[BLANK_AUDIO]"))
						// continue;
						
						JScribe.logger.info("Raw transcription ({} samples): {}", numRecordings, text);
						buffer.append(text);
						
						results.add(new Transcription(text, numRecordings));
					}
				}
				else
				{
					JScribe.logger.info("Abandoning sample (size {}, {} recordings)", toProcess.length, numRecordings);
				}
				
				// Run all success runnables
				runnables.forEach(Runnable::run);
				
				// Notify we're caught up
				lastTimestamp = firstTimestamp;
			}
		} catch(IOException e)
		{
			JScribe.logger.error("Failed to init whisper", e);
		} finally
		{
			// Ensure running is set to false
			shutdown();
		}
	}
	
	public void clearRecordings()
	{
		abandonSample.set(true);
		recordings.clear();
	}
	
	public void shutdown()
	{
		recordings.clear();
		running = false;
	}
	
	public int backlog()
	{
		return recordings.size();
	}
	
	public long getTimeBehind()
	{
		return System.currentTimeMillis() - lastTimestamp;
	}
	
	public List<Transcription> getTranscriptions()
	{
		List<Transcription> result = new ArrayList<>(results);
		results.clear();
		return result;
	}
	
	public void newRecording(Recording recording) throws InterruptedException
	{
		if(!running || Thread.interrupted())
		{
			throw new InterruptedException("Transcriber is dead");
		}
		
		recordings.add(recording);
	}
	
	static record Recording(long timeReceived, float[] samples, Runnable callback) {
		
		public Recording(float[] samples)
		{
			this(System.currentTimeMillis(), samples, null);
		}
	}
}
