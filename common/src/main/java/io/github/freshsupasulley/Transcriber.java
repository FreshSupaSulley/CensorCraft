package io.github.freshsupasulley;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import io.github.freshsupasulley.Transcriptions.Transcription;
import io.github.freshsupasulley.whisperjni.LibraryUtils;
import io.github.freshsupasulley.whisperjni.TokenData;
import io.github.freshsupasulley.whisperjni.WhisperContext;
import io.github.freshsupasulley.whisperjni.WhisperFullParams;
import io.github.freshsupasulley.whisperjni.WhisperJNI;
import io.github.freshsupasulley.whisperjni.WhisperState;
import io.github.freshsupasulley.whisperjni.WhisperVADContextParams;

/**
 * Transcriber waits for new audio samples and processes them into text segments using {@linkplain WhisperJNI}.
 */
class Transcriber extends Thread implements Runnable {
	
	// Maximum length a single transcription request can be
	private static final int MAX_LENGTH_MS = 30000;
	
	private final WhisperJNI whisper = new WhisperJNI();
	private final LinkedBlockingQueue<Recording> recordings = new LinkedBlockingQueue<Recording>();
	private final List<Transcription> results = new ArrayList<Transcription>();
	
	private final WhisperFullParams params;
	private final Path modelPath;
	
	private boolean running = true;
	private final Consumer<Boolean> onWarmedUp;
	private final AtomicBoolean abandonSample = new AtomicBoolean();
	
	private long lastTimestamp = System.currentTimeMillis();
	
	public Transcriber(Path modelPath, WhisperFullParams params, boolean useVulkan, Consumer<Boolean> onWarmedUp)
	{
		this.params = params;
		this.modelPath = modelPath;
		this.onWarmedUp = onWarmedUp;
		
		setName("JScribe Transcriber");
		setDaemon(true);
		
		try
		{
			// If we should use the standard natives OR we failed to load Vulkan
			if(!useVulkan || !tryLoadVulkan())
			{
				whisper.loadLibrary(JScribe.logger);
			}
			
			var logger = new DebugLogger(JScribe.logger);
			WhisperJNI.setLogger(logger); // use a logger wrapper because whisper logs are very verbose for VAD and idk how to configure logback from within a mod
		} catch(IOException e) // unsatisfiedlink errors should be wrapped into IOException
		{
			JScribe.logger.error("An error occurred loading natives (platform: {}, arch: {})", LibraryUtils.OS_NAME, LibraryUtils.OS_ARCH, e);
			throw new RuntimeException(e); // signals to JScribe to stop?
		}
	}
	
	private boolean tryLoadVulkan()
	{
		// Find the matching Vulkan natives folder name
		String resourceName = null;
		
		// We are only bundling certain natives into the library. Mac is notably excluded because Metal is already very fast on Apple Silicon at least...
		switch(LibraryUtils.getArchitecture())
		{
			case "x64":
			{
				if(LibraryUtils.isLinux())
				{
					resourceName = "linux-x64";
				}
				else if(LibraryUtils.isWindows())
				{
					resourceName = "windows-x64";
				}
				
				break;
			}
			case "arm64":
			{
				if(LibraryUtils.isLinux())
				{
					resourceName = "linux-arm64";
				}
			}
		}
		
		if(resourceName == null)
		{
			JScribe.logger.warn("Vulkan natives aren't available for this machine");
			return false;
		}
		
		if(LibraryUtils.findAndLoadVulkanRuntime())
		{
			JScribe.logger.info("Loaded the Vulkan runtime");
		}
		else
		{
			JScribe.logger.warn("Couldn't find a Vulkan runtime");
			return false;
		}
		
		// Now actually load the library
		try
		{
			Path tempFolder = LibraryUtils.extractFolderToTemp(JScribe.logger, Transcriber.class.getClassLoader().getResource("vulkan-natives/" + resourceName + "-vulkan-natives").toURI());
			LibraryUtils.loadLibrary(JScribe.logger, tempFolder);
			return true;
		} catch(IOException | URISyntaxException e)
		{
			JScribe.logger.error("Failed to load the Vulkan natives", e);
			return false;
		}
	}
	
	@Override
	public void run()
	{
		try(WhisperContext ctx = whisper.init(modelPath))
		{
			JScribe.logger.info("Warming up model");
			
			try(WhisperState state = whisper.initState(ctx))
			{
				// if this starts leaking over, just switch the warming up ONLY to state
				float[] samples = JScribe.readWavToFloatSamples(Transcriber.class.getClassLoader().getResourceAsStream("jfk.wav"));
				
				// Pass samples to whisper
				int result = whisper.fullWithState(ctx, state, params, samples, samples.length);
				
				if(result != 0)
				{
					throw new IllegalStateException("Whisper failed with code " + result);
				}
				
				for(int i = 0; i < whisper.fullNSegmentsFromState(state); i++)
				{
					TokenData[] tokens = whisper.getTokensFromState(ctx, state, i);
					JScribe.logger.debug("Warm-up transcription: {} tokens", tokens.length);
				}
				
				// Signals to JScribe we're done warming up
				onWarmedUp.accept(true);
			} catch(Exception e)
			{
				JScribe.logger.warn("Failed to warm up model", e);
				onWarmedUp.accept(false);
			}
			
			while(running)
			{
				abandonSample.set(false);
				
				if(recordings.isEmpty())
				{
					lastTimestamp = System.currentTimeMillis();
					continue;
				}
				
				float[] cumulativeFullWindow = new float[(int) (JScribe.FORMAT.getSampleRate() * (MAX_LENGTH_MS / 1000f))];
				
				int sampleIndex = 0;
				int numRecordings = 0;
				long firstTimestamp = 0;
				
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
				// toProcess = trimWithTarsos(toProcess, -30);
				
				// Pad to 1000ms (refer to whisper.cpp in whisper in whisper-jni 1.7.1 [whisper btw])
				final int minLength = (int) (JScribe.FORMAT.getSampleRate() * (1050f / 1000)); // because x1f somehow got 990
				
				if(toProcess.length < minLength)
				{
					JScribe.logger.trace("Padding window with {} zeros", (minLength - toProcess.length));
					
					float[] paddedWindow = new float[minLength];
					System.arraycopy(toProcess, 0, paddedWindow, 0, toProcess.length);
					toProcess = paddedWindow;
				}
				
				// final float[] samples2 = toProcess;
				// Thread thread = new Thread(() ->
				// {
				// try
				// {
				// JScribe.writeWavFile(16000, samples2, Path.of("src/test/resources/test" + ".wav"));
				// } catch(IOException e)
				// {
				// // TODO Auto-generated catch block
				// e.printStackTrace();
				// }
				// });
				// thread.start();
				
				JScribe.logger.debug("Transcribing {} recordings (length {})", numRecordings, toProcess.length);
				
				try(WhisperState state = whisper.initState(ctx))
				{
					// Pass samples to whisper
					// int result = whisper.full(ctx, params, toProcess, toProcess.length);
					//
					// if(result != 0)
					// {
					// JScribe.logger.error("Whisper failed with code {}", result);
					// continue;
					// }
					//
					// int numSegments = whisper.fullNSegments(ctx);
					String result = whisper.vadState(ctx, state, params, new WhisperVADContextParams(), toProcess, toProcess.length);
					
					if(result == null)
					{
						JScribe.logger.debug("No voice activity detected");
					}
					else
					{
						JScribe.logger.debug("Raw transcription ({} recordings): {}", numRecordings, result);
						
						// does it need to be atomic?
						if(!abandonSample.get())
						{
							results.add(new Transcription(result, numRecordings, System.currentTimeMillis() - startTime));
						}
						else
						{
							JScribe.logger.debug("Abandoning sample (size {}, {} recordings)", toProcess.length, numRecordings);
						}
					}
					
					// Notify we're caught up
					lastTimestamp = firstTimestamp;
				}
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
	
	// public static float[] trimWithTarsos(float[] samples, double silenceThresholdDb)
	// {
	// SilenceDetector detector = new SilenceDetector(silenceThresholdDb, false);
	//
	// // Convert your float[] samples to byte[] for the audio stream
	// byte[] byteData = JScribe.floatToPCM(samples); // assumes PCM 16-bit little-endian
	// UniversalAudioInputStream inputStream = new UniversalAudioInputStream(new ByteArrayInputStream(byteData), JScribe.FORMAT);
	//
	// // Create dispatcher: 1024 sample frames, 512 overlap
	// AudioDispatcher dispatcher = new AudioDispatcher(inputStream, 1024, 512);
	//
	// AtomicInteger firstNonSilentFrame = new AtomicInteger(-1);
	// AtomicInteger lastNonSilentFrame = new AtomicInteger(-1);
	// AtomicInteger frameCount = new AtomicInteger(0);
	//
	// dispatcher.addAudioProcessor(detector);
	//
	// // Attach silence detection processor
	// dispatcher.addAudioProcessor(new AudioProcessor()
	// {
	// @Override
	// public boolean process(AudioEvent audioEvent)
	// {
	// double dB = detector.currentSPL();
	//
	// if(dB > silenceThresholdDb)
	// {
	// if(firstNonSilentFrame.get() == -1)
	// {
	// firstNonSilentFrame.set(frameCount.get());
	// }
	//
	// lastNonSilentFrame.set(frameCount.get());
	// }
	//
	// frameCount.incrementAndGet();
	// return true;
	// }
	//
	// @Override
	// public void processingFinished()
	// {
	// // no-op
	// }
	// });
	//
	// // Run the dispatcher (blocking call)
	// dispatcher.run();
	//
	// // If no non-silent audio found
	// if(firstNonSilentFrame.get() == -1)
	// {
	// return new float[0];
	// }
	//
	// // Compute sample range
	// int frameSize = 1024;
	// int startSample = firstNonSilentFrame.get() * frameSize;
	// int endSample = Math.min(samples.length, (lastNonSilentFrame.get() + 1) * frameSize);
	//
	// // Return trimmed sample array
	// return Arrays.copyOfRange(samples, startSample, endSample);
	// }
	
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
	
	record Recording(long timeReceived, float[] samples) {
		
		public Recording(float[] samples)
		{
			this(System.currentTimeMillis(), samples);
		}
	}
}
