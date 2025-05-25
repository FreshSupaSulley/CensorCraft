package io.github.freshsupasulley;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.freshsupasulley.Transcriber.Recording;
import io.github.givimad.libfvadjni.VoiceActivityDetector;
import io.github.givimad.libfvadjni.VoiceActivityDetector.Mode;

/**
 * The entry point of the JScribe library.
 */
public class JScribe implements UncaughtExceptionHandler {
	
	static Logger logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
	
	// Required
	private final Path modelPath;
	private final String microphone;
	private final long latency, overlap;
	
	// VAD
	private final VoiceActivityDetector.Mode mode;
	private final float inputSensitivity;
	private final boolean vad;
	
	private final boolean denoise, useVulkan, warmUpModel, noLoadNatives;
	private AudioRecorder recorder;
	private Transcriber transcriber;
	
	private volatile State state;
	
	/**
	 * Gets all available Whisper models in GGML format from Hugging Face. Useful to pass into {@link JScribe#downloadModel}.
	 * 
	 * @return array of model names
	 * @throws IOException if something went wrong
	 */
	public static Model[] getModels() throws IOException
	{
		String apiUrl = "https://huggingface.co/api/models/ggerganov/whisper.cpp/tree/main";
		
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(apiUrl)).build();
		
		try(HttpClient client = HttpClient.newHttpClient())
		{
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			
			if(response.statusCode() != 200)
			{
				throw new IOException("Failed to fetch model list. HTTP status: " + response.statusCode());
			}
			
			JSONArray files = new JSONArray(response.body());
			List<Model> models = new ArrayList<Model>();
			
			// This only takes proper models (not encoding ones)
			Pattern pattern = Pattern.compile("ggml-(.+?)\\.bin");
			
			for(int i = 0; i < files.length(); i++)
			{
				JSONObject json = files.getJSONObject(i);
				Matcher matcher = pattern.matcher(json.getString("path"));
				
				if(matcher.find())
				{
					models.add(new Model(matcher.group(1), json.getLong("size")));
				}
			}
			
			return models.toArray(Model[]::new);
		} catch(InterruptedException e)
		{
			throw new IOException(e);
		}
	}
	
	/**
	 * Gets the basic information about a model from Hugging Face, or <code>null</code> if the model wasn't found. Useful if you have hardcoded strings of model
	 * names and want to ensure it's still hosted.
	 * 
	 * @param modelName name of the model
	 * @return {@linkplain Model} instance, or <code>null</code> if the model wasn't found.
	 * @throws IOException if something went wrong
	 */
	public static Model getModelInfo(String modelName) throws IOException
	{
		return Stream.of(getModels()).filter(model -> model.name().equals(modelName)).findFirst().orElse(null);
	}
	
	/**
	 * Prepares a {@link ModelDownloader} instance to download the Whisper model in GGML format from Hugging Face.
	 * 
	 * @param modelName   name of the model (use {@link JScribe#getModels()})
	 * @param destination output path
	 * @param onComplete  biconsumer that runs when complete. First param indicates true if successful or false otherwise (failed or cancelled), second param is the
	 *                    exception that caused the error, if any. A {@link CancellationException} indicates it was cancelled.
	 * @return {@link ModelDownloader} object to manage the download
	 */
	public static ModelDownloader downloadModel(String modelName, Path destination, BiConsumer<Boolean, Throwable> onComplete)
	{
		return new ModelDownloader(modelName, destination, onComplete);
	}
	
	/**
	 * Returns the compatible microphones that support the desired {@link AudioFormat} required for transcription.
	 * 
	 * @return list of mixers. Can be empty
	 */
	public static List<Mixer.Info> getMicrophones()
	{
		List<Mixer.Info> names = new ArrayList<Mixer.Info>();
		Mixer.Info[] mixers = AudioSystem.getMixerInfo();
		
		for(Mixer.Info mixerInfo : mixers)
		{
			Mixer mixer = AudioSystem.getMixer(mixerInfo);
			DataLine.Info lineInfo = new DataLine.Info(TargetDataLine.class, AudioRecorder.FORMAT);
			
			if(mixer.isLineSupported(lineInfo))
			{
				names.add(mixerInfo);
			}
		}
		
		return names;
	}
	
	private JScribe(Logger logger, Path modelPath, String microphone, long latency, long overlap, float inputSensitivity, boolean vad, Mode mode, boolean denoise, boolean useVulkan, boolean warmUpModel, boolean noLoadNatives)
	{
		JScribe.logger = logger;
		this.modelPath = modelPath;
		this.microphone = microphone;
		this.latency = latency;
		this.overlap = overlap;
		this.inputSensitivity = inputSensitivity;
		this.vad = vad;
		this.mode = mode;
		this.denoise = denoise;
		this.useVulkan = useVulkan;
		this.warmUpModel = warmUpModel;
		this.noLoadNatives = noLoadNatives;
	}
	
	/**
	 * Starts live audio transcription. Use {@link #stop()} when finished.
	 * 
	 * <p>
	 * You cannot run multiple instances at a time. Use {@linkplain #isInUse()} before to ensure JScribe is ready to start.
	 * </p>
	 * 
	 * @throws NoMicrophoneException if no usable microphones were found
	 * @throws IOException           if {@link #isInUse()} or something went wrong
	 */
	public synchronized void start() throws IOException, NoMicrophoneException
	{
		// Ensure only one process can use JScribe at a time (otherwise causes JNI crashes). Maybe this crash is on a per-model basis, unsure...
		// Null state means not started or stopped
		if(isInUse())
		{
			throw new IOException("JScribe already started. Wait for it to die");
		}
		
		try
		{
			state = State.INITIALIZING;
			
			logger.info("Starting JScribe");
			
			recorder = new AudioRecorder(transcriber = new Transcriber(modelPath, useVulkan, noLoadNatives), microphone, latency, overlap, true, inputSensitivity, vad, mode, denoise);
			
			// Report errors to this thread
			recorder.setUncaughtExceptionHandler(this);
			transcriber.setUncaughtExceptionHandler(this);
			
			// We need the transcriber to start first
			transcriber.start();
			
			CompletableFuture<Void> future = new CompletableFuture<Void>();
			
			// If we enabled warming up the model
			if(warmUpModel)
			{
				try
				{
					logger.info("Warming up transcriber");
					transcriber.newRecording(new Recording(System.currentTimeMillis(), readWavToFloatSamples(JScribe.class.getClassLoader().getResourceAsStream("jfk.wav")), () ->
					{
						logger.info("Warming up completed");
						future.complete(null);
					}));
				} catch(IOException | UnsupportedAudioFileException e)
				{
					future.completeExceptionally(e);
				}
			}
			else
			{
				// No warm-up, skip it
				future.complete(null);
			}
			
			// Start recording AFTER we're warmed up
			future.orTimeout(10, TimeUnit.SECONDS).exceptionally(e ->
			{
				logger.error("Failed to warm-up transcriber", e);
				return null;
			}).thenRun(() ->
			{
				// synchronized(this)
				{
					// If the transcriber somehow died
					if(!transcriber.isRunning())
					{
						logger.warn("JScribe was stopped during warm-up");
						return;
					}
					
					// Empty the transcription array if warm-up succeeded
					getTranscriptions();
					recorder.start();
					state = State.RUNNING;
				}
			});
		} catch(Exception e)
		{
			logger.error("Failed to start JScribe", e);
			stop();
			throw e;
		}
	}
	
	/**
	 * Stops and waits for JScribe to shutdown completely. It's recommended you use {@linkplain #stop()} instead to wait indefinitely.
	 * 
	 * <p>
	 * If an {@linkplain InterruptedException} is thrown, don't attempt to restart JScribe as it could cause a JVM crash if multiple transcribers are working.
	 * </p>
	 * 
	 * @param wait maximum time to wait before exiting early
	 * @throws InterruptedException if worker threads failed to end in time
	 */
	public synchronized void stop(Duration wait) throws InterruptedException
	{
		if(state == null || isShuttingDown())
		{
			return;
		}
		
		try
		{
			logger.info("Stopping JScribe");
			state = State.STOPPING;
			
			if(recorder != null)
				recorder.shutdown();
			if(transcriber != null)
				transcriber.shutdown();
			
			// Wait to die
			logger.info("Waiting for threads to die (max wait: {}s)", wait.toSeconds());
			
			long millis = wait.toMillis();
			kill(recorder, millis);
			kill(transcriber, millis);
			// if(recorder != null && recorder.getState() != Thread.State.NEW)
			// recorder.join(wait);
			// if(transcriber != null && recorder.getState() != Thread.State.NEW)
			// transcriber.join(wait);
			
			JScribe.logger.info("Stopped JScribe");
		} catch(InterruptedException e)
		{
			boolean transcriberAlive = Optional.ofNullable(transcriber).map(Transcriber::isAlive).orElse(false);
			JScribe.logger.error("Failed to join JScribe threads (recorder alive: {}, transcriber alive: {})", Optional.ofNullable(recorder).map(AudioRecorder::isAlive).orElse(false), transcriberAlive, e);
			
			if(transcriberAlive)
			{
				JScribe.logger.error("Transcriber still being alive could indicate transcription is still wrapping up");
			}
			
			throw e;
		} catch(Exception e)
		{
			JScribe.logger.info("An error occurred stopping JScribe", e);
		} finally
		{
			state = null;
		}
	}
	
	private void kill(Thread thread, long millis) throws InterruptedException
	{
		if(thread != null && thread.getState() != Thread.State.NEW)
		{
			JScribe.logger.trace("Killing " + thread.getName());
			
			thread.join(millis);
			
			if(thread.isAlive())
			{
				throw new IllegalStateException("Failed to kill " + thread.getName());
			}
		}
	}
	
	/**
	 * Stops and waits indefinitely for JScribe to shutdown completely.
	 */
	public void stop()
	{
		try
		{
			stop(Duration.ZERO);
		} catch(InterruptedException e)
		{
			logger.error("Received interrupted exception, but passed 0 duration?", e);
		}
	}
	
	/**
	 * Clears the transcription recording queue and abandons the current recording.
	 */
	public void reset()
	{
		if(isRunning())
		{
			logger.info("Resetting JScribe");
			recorder.clear();
			transcriber.reset();
		}
		else
		{
			logger.warn("Can't reset JScribe. Not running");
		}
	}
	
	/**
	 * Gets the number of audio samples in the queue waiting to be processed.
	 * 
	 * @return number of audio samples
	 */
	public int getTranscriptionBacklog()
	{
		return transcriber.backlog();
	}
	
	/**
	 * Initializing is the stage before running, where JScribe loads the model and starts worker threads. If configured, also runs a "warm-up" audio sample to the
	 * transcriber.
	 * 
	 * @return true if JScribe is initializing, false otherwise
	 */
	public boolean isInitializing()
	{
		return state == State.INITIALIZING;
	}
	
	/**
	 * Returns true if transcribing live audio.
	 * 
	 * @return true if running, false otherwise
	 */
	public boolean isRunning()
	{
		return state == State.RUNNING;
	}
	
	/**
	 * Returns true if JScribe is shutting down.
	 * 
	 * @return true if shutting down, false otherwise
	 */
	public boolean isShuttingDown()
	{
		return state == State.STOPPING;
	}
	
	/**
	 * Returns true if JScribe is alive in any capacity. This means a worker thread may still be running.
	 * 
	 * @return true if alive, false otherwise
	 */
	public boolean isInUse()
	{
		return state != null;
	}
	
	// /**
	// * Returns true if no audio is being received, meaning there's an open stream to a microphone but it's not returning any data. This can happen when running in
	// * the Eclipse IDE on macOS due to permission problems. Ensure this is running with {@link JScribe#isRunning()} first, otherwise it will always return true.
	// *
	// * @return true if we're not receiving any audio data from the client, or JScribe is not running
	// */
	// public boolean noAudio()
	// {
	// return !isRunning() || !recorder.receivingAudio();
	// }
	
	/**
	 * True if the currently recording audio sample meets the minimum volume level and voice is detected (if enabled), false otherwise.
	 * 
	 * @return true if live audio meets minimum volume level, and if VAD detected the user is speaking if enabled
	 */
	public boolean audioMeetsCriteria()
	{
		return isRunning() && recorder.audioMeetsCriteria();
	}
	
	/**
	 * Returns the current volume using RMS (0 when not running).
	 * 
	 * @return audio level of samples, [0 - 1].
	 */
	public float getAudioLevel()
	{
		if(!isRunning())
			return 0;
		return recorder.getAudioLevel();
	}
	
	/**
	 * Returns the active microphone information with {@link Mixer.Info}.
	 * 
	 * @return microphone information, or null if not running
	 */
	public Mixer.Info getActiveMicrophone()
	{
		if(!isRunning())
			return null;
		
		return recorder.getMicrophoneInfo();
	}
	
	/**
	 * Returns the amount of time transcription is taking while there's a backlog of recordings to process, in milliseconds. Used for debugging purposes.
	 * 
	 * @return milliseconds behind the current audio feed
	 */
	public long getTimeBehind()
	{
		return transcriber.getTimeBehind();
	}
	
	/**
	 * Returns the last time an audio recording began.
	 * 
	 * @return last time (in epoch millis) an audio recording began
	 */
	public long getLastAudioRecordingTimestamp()
	{
		return recorder.getLastTimestamp();
	}
	
	/**
	 * Gets all transcriptions and clears the buffer.
	 * 
	 * @return buffer of transcriptions (can be empty)
	 */
	public Transcriptions getTranscriptions()
	{
		return new Transcriptions(transcriber.getTranscriptions());
	}
	
	@Override
	public void uncaughtException(Thread t, Throwable e)
	{
		JScribe.logger.error("JScribe ended early due to an unhandled error in thread " + t.getName(), e);
		stop();
	}
	
	@Override
	public String toString()
	{
		return super.toString() + " - State: " + state;
	}
	
	/**
	 * Helper class to build JScribe instances.
	 */
	public static class Builder {
		
		// Required
		private final Path modelPath;
		
		private String microphone;
		private long latency, overlap;
		
		private long minWindowContext;
		
		// VAD
		private VoiceActivityDetector.Mode mode;
		private boolean vad;
		
		private float inputSensitivity;
		
		private boolean denoise = true, vulkan, warmUpModel, noLoadNatives;
		
		/**
		 * Creates a new JScribe Builder instance. All parameters in this constructor are the minimum required parameters to build a simple instance.
		 * 
		 * <p>
		 * <code>overlap</code> can also be thought of as the length of an individual audio sample. For example, setting latency to 500ms and overlap to 5000ms would
		 * have transcription results coming back every 500ms, but those results use the full 5000ms context.
		 * </p>
		 * 
		 * @param modelPath path to GGML formatted Whisper model
		 * @param latency   audio sample length in milliseconds. Must be at least 30ms. If low, set overlap to be much higher to catch full words.
		 * @param overlap   extra audio in milliseconds. Samples are collected into a window of this size for context to the transcriber. Must be non-negative.
		 * @throws IllegalArgumentException if {@code latency < 30} or {@code overlap < 0}
		 */
		public Builder(Path modelPath, long latency, long overlap)
		{
			this.modelPath = modelPath;
			
			if(latency < 30)
			{
				throw new IllegalArgumentException("Latency must be at least 30ms");
			}
			
			if(overlap < 0)
			{
				throw new IllegalArgumentException("Overlap must be non-negative");
			}
			
			this.latency = latency;
			this.overlap = overlap;
		}
		
//		/**
//		 * Defines the minimum amount of audio (in milliseconds) the window must fill to before transcribing the entire window.
//		 * 
//		 * <p>
//		 * To only transcribe a full window, set <code>minWindowContext</code> to match the latency.
//		 * </p>
//		 * 
//		 * @param minWindowContext
//		 * @return
//		 */
//		public Builder minimumWindowContext(long minWindowContext)
//		{
//			if(minWindowContext < latency)
//				throw new IllegalArgumentException("Minimum window context must be >= latency");
//			
//			this.minWindowContext = minWindowContext;
//			return this;
//		}
		
		/**
		 * Sets the input sensitivity that any one sample's volume of an audio recording must match in order to be transcribed. This can be used either with or instead
		 * of {@link #enableVAD(Mode)}.
		 * 
		 * @param sensitivity minimum volume level, [0 - 1.0f]
		 * @return this, for chaining
		 */
		public Builder setInputSensitivity(float sensitivity)
		{
			this.inputSensitivity = Math.clamp(sensitivity, 0, 1);
			return this;
		}
		
		/**
		 * Tries to use the microphone specified by its name. Must match the name <b>exactly</b>. Use {@link JScribe#getMicrophones()} to get the list of accepted ones.
		 * 
		 * <p>
		 * If the microphone by the provided name isn't found, it uses the first one available.
		 * </p>
		 * 
		 * @param microphone preferred microphone name (must be exact)
		 * @return this, for chaining
		 */
		public Builder setPreferredMicrophone(String microphone)
		{
			this.microphone = microphone;
			return this;
		}
		
		/**
		 * Voice activity detection. If enabled, does not process audio samples unless it detects the user is speaking.
		 * 
		 * @param mode {@link VoiceActivityDetector.Mode} for determining aggressiveness
		 * @return this, for chaining
		 */
		public Builder enableVAD(VoiceActivityDetector.Mode mode)
		{
			Objects.requireNonNull(mode);
			this.vad = true;
			this.mode = mode;
			return this;
		}
		
		/**
		 * Denoising the audio samples. Helps with VAD. On by default.
		 * 
		 * @param denoise true to denoise samples, false otherwise
		 * @return this, for chaining
		 */
		public Builder denoise(boolean denoise)
		{
			this.denoise = denoise;
			return this;
		}
		
		/**
		 * Sets the logger all JScribe operations will use.
		 * 
		 * @param logger {@link Logger} instance
		 * @return this, for chaining
		 */
		public Builder setLogger(Logger logger)
		{
			Objects.requireNonNull(logger);
			JScribe.logger = logger;
			return this;
		}
		
		/**
		 * Passes dummy audio into transcriber to warm-up the model (considered part of initialization).
		 * 
		 * @return this, for chaining
		 */
		public Builder warmUpModel()
		{
			this.warmUpModel = true;
			return this;
		}
		
		/**
		 * Prevents loading any built-in natives and therefore allows for customizing loading <code>whisper-jni</code> natives (and its dependencies), but the
		 * implementor is responsible for ensuring it will work.
		 * 
		 * <p>
		 * <b>Don't use unless you know what you're doing! </b>
		 * </p>
		 * 
		 * @return this, for chaining
		 */
		public Builder skipLoadingNatives()
		{
			this.noLoadNatives = true;
			return this;
		}
		
		/**
		 * Enables Vulkan support <b>only</b> if using Windows AMD 64.
		 * 
		 * @return this, for chaining
		 */
		public Builder useVulkan()
		{
			if(!LibraryLoader.canUseVulkan())
			{
				logger.error("Can't use Vulkan, wrong platform / arch ({}, {})", LibraryLoader.OS_NAME, LibraryLoader.OS_ARCH);
				return this;
			}
			
			this.vulkan = true;
			return this;
		}
		
		/**
		 * Builds a new JScribe instance. Use {@linkplain JScribe#start} to begin live audio transcription.
		 * 
		 * @return new {@linkplain JScribe} instance
		 */
		public JScribe build()
		{
			return new JScribe(logger, modelPath, microphone, latency, overlap, inputSensitivity, vad, mode, denoise, vulkan, warmUpModel, noLoadNatives);
		}
	}
	
	private static enum State
	{
		INITIALIZING, RUNNING, STOPPING;
	}
	
	private static float[] readWavToFloatSamples(InputStream stream) throws IOException, UnsupportedAudioFileException
	{
		// Decode WAV header and get PCM stream
		AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new BufferedInputStream(stream)); // https://stackoverflow.com/questions/5529754/java-io-ioexception-mark-reset-not-supported
		
		// Create a short buffer with proper byte order (little endian)
		ByteBuffer byteBuffer = ByteBuffer.wrap(audioInputStream.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN);
		ShortBuffer shortBuffer = byteBuffer.asShortBuffer();
		
		// Allocate the float array
		float[] samples = new float[shortBuffer.remaining()];
		
		for(int i = 0; i < samples.length; i++)
		{
			// Normalize the short to a float between -1 and 1
			samples[i] = shortBuffer.get() / 32768f;
		}
		
		return samples;
	}
}
