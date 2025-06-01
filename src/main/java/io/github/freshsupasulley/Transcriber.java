package io.github.freshsupasulley;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.SilenceDetector;
import be.tarsos.dsp.io.UniversalAudioInputStream;
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
	private final List<Transcription> results = new ArrayList<Transcription>();
	
	private static final WhisperFullParams params;
	
	private final Path modelPath;
	
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
	}
	
	public Transcriber(Path modelPath, boolean useVulkan, boolean noLoadNatives)
	{
		this.modelPath = modelPath;
		
		setName("JScribe Transcriber");
		setDaemon(true);
		
		if(noLoadNatives)
		{
			JScribe.logger.warn("Not loading built-in natives");
			return;
		}
		
		try
		{
			if(useVulkan && LibraryLoader.canUseVulkan())
			{
				LibraryLoader.loadVulkan();
			}
			else
			{
				JScribe.logger.info("Loading built-in whisper-jni natives");
				
				LibraryUtils.loadLibrary(JScribe.logger::debug);
				// Then test loading whisper
				WhisperJNI.setLibraryLogger(JScribe.logger::debug);
			}
		} catch(IOException | UnsatisfiedLinkError e)
		{
			JScribe.logger.error("An error occurred loading natives (platform: {}, arch: {})", System.getProperty("os.name"), System.getProperty("os.arch"), e);
			throw new RuntimeException(e); // signals to JScribe to stop?
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
				
				float[] cumulativeFullWindow = new float[(int) (JScribe.FORMAT.getSampleRate() * (MAX_LENGTH_MS / 1000f))];
				
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
					
					if(sampleIndex + sampleLength > cumulativeFullWindow.length)
					{
						JScribe.logger.warn("Tried to transcribe audio longer than {}ms", MAX_LENGTH_MS);
						break; // abandon the next samples who cares
					}
					
					// Copy samples from this recording into cumulative window
					System.arraycopy(sample.samples(), 0, cumulativeFullWindow, sampleIndex, sampleLength);
					sampleIndex += sampleLength;
					
					// Also keep track of any completables
					if(sample.callback() != null)
					{
						runnables.add(sample.callback());
					}
				}
				
				// This can happen as recordings could get cleared at any time
				if(numRecordings == 0)
				{
					continue;
				}
				
				long startTime = System.currentTimeMillis();
				
				// Now merge into one
				float[] toProcess = new float[sampleIndex];
				System.arraycopy(cumulativeFullWindow, 0, toProcess, 0, sampleIndex);
				
				// Trim it down to remove silence
//				toProcess = trimWithTarsos(toProcess, -30);
				
				// Pad to 1000ms (refer to whisper.cpp in whisper in whisper-jni 1.7.1 [whisper btw])
				final int minLength = (int) (JScribe.FORMAT.getSampleRate() * (1050f / 1000)); // because x1f somehow got 990
				
				if(toProcess.length < minLength)
				{
					JScribe.logger.trace("Padding window with {} zeros", (minLength - toProcess.length));
					
					float[] paddedWindow = new float[minLength];
					System.arraycopy(toProcess, 0, paddedWindow, 0, toProcess.length);
					toProcess = paddedWindow;
				}
				
//				final float[] samples2 = toProcess;
//				Thread thread = new Thread(() ->
//				{
//					try
//					{
//						JScribe.writeWavFile(16000, samples2, Path.of("src/test/resources/test" + ".wav"));
//					} catch(IOException e)
//					{
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				});
//				thread.start();
				
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
					List<Transcription> transcriptions = new ArrayList<Transcription>(numSegments);
					
					for(int i = 0; i < numSegments; i++)
					{
						String text = whisper.fullGetSegmentTextFromState(state, i).trim();
						// can suppress this in whisper context
						// if(text.equals("."))
						// continue;
						// if(text.equals("[BLANK_AUDIO]"))
						// continue;
						
						JScribe.logger.debug("Raw transcription ({} samples): {}", numRecordings, text);
						
						transcriptions.add(new Transcription(text, numRecordings, System.currentTimeMillis() - startTime));
					}
					
					// because I want to add all transcriptions at the same time to not freak out the results
					results.addAll(transcriptions);
				}
				else
				{
					JScribe.logger.debug("Abandoning sample (size {}, {} recordings)", toProcess.length, numRecordings);
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
	
	public static float[] trimWithTarsos(float[] samples, double silenceThresholdDb)
	{
		SilenceDetector detector = new SilenceDetector(silenceThresholdDb, false);
		
		// Convert your float[] samples to byte[] for the audio stream
		byte[] byteData = JScribe.floatToPCM(samples); // assumes PCM 16-bit little-endian
		UniversalAudioInputStream inputStream = new UniversalAudioInputStream(new ByteArrayInputStream(byteData), JScribe.FORMAT);
		
		// Create dispatcher: 1024 sample frames, 512 overlap
		AudioDispatcher dispatcher = new AudioDispatcher(inputStream, 1024, 512);
		
		AtomicInteger firstNonSilentFrame = new AtomicInteger(-1);
		AtomicInteger lastNonSilentFrame = new AtomicInteger(-1);
		AtomicInteger frameCount = new AtomicInteger(0);
		
		dispatcher.addAudioProcessor(detector);
		
		// Attach silence detection processor
		dispatcher.addAudioProcessor(new AudioProcessor()
		{
			@Override
			public boolean process(AudioEvent audioEvent)
			{
				double dB = detector.currentSPL();
				
				if(dB > silenceThresholdDb)
				{
					if(firstNonSilentFrame.get() == -1)
					{
						firstNonSilentFrame.set(frameCount.get());
					}
					
					lastNonSilentFrame.set(frameCount.get());
				}
				
				frameCount.incrementAndGet();
				return true;
			}
			
			@Override
			public void processingFinished()
			{
				// no-op
			}
		});
		
		// Run the dispatcher (blocking call)
		dispatcher.run();
		
		// If no non-silent audio found
		if(firstNonSilentFrame.get() == -1)
		{
			return new float[0];
		}
		
		// Compute sample range
		int frameSize = 1024;
		int startSample = firstNonSilentFrame.get() * frameSize;
		int endSample = Math.min(samples.length, (lastNonSilentFrame.get() + 1) * frameSize);
		
		// Return trimmed sample array
		return Arrays.copyOfRange(samples, startSample, endSample);
	}
	
	public void reset()
	{
		JScribe.logger.debug("Clearing recordings and requesting next sample to be abandoned");
		recordings.clear();
		abandonSample.set(true);
	}
	
	public void clearRecordings()
	{
		recordings.clear();
	}
	
	public void shutdown()
	{
		JScribe.logger.debug("Transcription shutting down");
		recordings.clear(); // do NOT call reset. Creates endless loop
		running = false;
	}
	
	public boolean isRunning()
	{
		return running;
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
	
	public void newRecording(Recording recording)
	{
		if(!running || Thread.interrupted())
		{
			throw new IllegalStateException("Transcriber is dead");
		}
		
		JScribe.logger.debug("Received new recording");
		recordings.add(recording);
	}
	
	static record Recording(long timeReceived, float[] samples, Runnable callback) {
		
		public Recording(float[] samples)
		{
			this(System.currentTimeMillis(), samples, null);
		}
	}
}
