package io.github.freshsupasulley;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import io.github.freshsupasulley.Transcriptions.Transcription;
import io.github.givimad.whisperjni.WhisperContext;
import io.github.givimad.whisperjni.WhisperFullParams;
import io.github.givimad.whisperjni.WhisperJNI;
import io.github.givimad.whisperjni.internal.LibraryUtils;

/**
 * Transcriber waits for new audio samples and processes them into text segments using {@linkplain WhisperJNI}.
 */
class Transcriber extends Thread implements Runnable {
	
	private final WhisperJNI whisper = new WhisperJNI();
	private final LinkedBlockingQueue<Recording> recordings = new LinkedBlockingQueue<Recording>();
	private final StringBuffer buffer = new StringBuffer();
	private final List<Transcription> results = new ArrayList<Transcription>();
	
	private static final WhisperFullParams params;
	
	private Path modelPath;
	private boolean running = true, processing;
	
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
		
		Transcriber.loadWhisperJNI();
	}
	
	private static void loadWhisperJNI()
	{
		JScribe.logger.info("Loading libraries");
		
		try
		{
			// Not dealing with this unless I wanna fork it (hell naww)
			// System.setProperty("io.github.givimad.whisperjni.libdir", LibraryLoader.extractToTemp().toString());
			
			// path needs to be valid on windows, opened a pr :(
			LibraryUtils.loadLibrary(JScribe.logger::debug);
			
			// LibraryLoader.loadBundledNatives((a, b) ->
			// {
			// String nameA = a.getName().toLowerCase();
			// String nameB = b.getName().toLowerCase();
			//
			// int difference = Integer.compare(getPriority(nameA), getPriority(nameB));
			//
			// // Assort by name for consistency otherwise
			// return difference == 0 ? nameA.compareTo(nameB) : difference;
			// });
			
			// Then test loading whisper
			WhisperJNI.setLibraryLogger(null);
		} catch(IOException | UnsatisfiedLinkError e)
		{
			JScribe.logger.error("An error occurred loading natives (platform: {}, arch: {})", System.getProperty("os.name"), System.getProperty("os.arch"), e);
			System.exit(1);
		}
	}
	
	// private static int getPriority(String name)
	// {
	// // 0 == highest priority
	// // For simplicity we're only loading the full static version so no need to differentiate windows natives
	// // if(name.contains("_full"))
	// // return 0;
	// if(name.contains("ggml"))
	// return 0;
	// // Load last
	// if(name.contains("jni"))
	// return 2;
	//
	// // Anything else can load at whatever order
	// return 1;
	// }
	
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
				if(recordings.size() == 0)
				{
					processing = false;
					lastTimestamp = System.currentTimeMillis();
					continue;
				}
				
				processing = true;
				
				// Squash all requests into one batch to stay up to date
				final int numRecordings = recordings.size();
				float[][] collectRecordings = new float[numRecordings][];
				long firstTimestamp = 0;
				int bufferSize = 0;
				
				for(int i = 0; i < numRecordings; i++)
				{
					Recording sample = recordings.take();
					collectRecordings[i] = sample.samples();
					
					// The first timestamp is all we care about
					if(firstTimestamp == 0)
					{
						firstTimestamp = sample.timestamp();
					}
					
					bufferSize += collectRecordings[i].length;
				}
				
				// Now merge into one
				float[] toProcess = new float[bufferSize];
				
				for(int bufferIndex = 0, i = 0; i < numRecordings; i++)
				{
					System.arraycopy(collectRecordings[i], 0, toProcess, bufferIndex, collectRecordings[i].length);
					bufferIndex += collectRecordings[i].length;
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
				
				JScribe.logger.debug("Transcribing {} recordings", numRecordings);
				
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
					// if(text.equals("."))
					// continue;
					// if(text.equals("[BLANK_AUDIO]"))
					// continue;
					
					JScribe.logger.info("Raw transcription ({} samples): {}", numRecordings, text);
					buffer.append(text);
					
					results.add(new Transcription(text, numRecordings));
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
		recordings.clear();
		running = false;
	}
	
	public long getTimeBehind()
	{
		return !processing ? 0 : System.currentTimeMillis() - lastTimestamp;
	}
	
	public List<Transcription> getTranscriptions()
	{
		List<Transcription> result = new ArrayList<>(results);
		results.clear();
		return result;
	}
	
	public void newRecording(float[] sample) throws InterruptedException
	{
		if(!running || Thread.interrupted())
		{
			throw new InterruptedException("Transcriber is dead");
		}
		
		recordings.add(new Recording(System.currentTimeMillis(), sample));
	}
	
	private static record Recording(long timestamp, float[] samples) {
	}
}
