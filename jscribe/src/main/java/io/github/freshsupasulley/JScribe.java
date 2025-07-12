package io.github.freshsupasulley;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
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

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.freshsupasulley.Transcriber.Recording;
import io.github.freshsupasulley.whisperjni.LibraryUtils;
import io.github.freshsupasulley.whisperjni.WhisperFullParams;

/**
 * The entry point of the JScribe library.
 */
public class JScribe implements UncaughtExceptionHandler {
	
	/** The format Whisper wants (wave file) */
	public static final AudioFormat FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000, 16, 1, 2, 16000, false);
	// public static final TarsosDSPAudioFormat FORMAT = new TarsosDSPAudioFormat(16000, 16, 1, true, false);
	
	static Logger logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
	
	// Required
	private final Path modelPath;
	private final WhisperFullParams params;
	private final boolean useVulkan, warmUpModel, noLoadNatives;
	private Transcriber transcriber;
	
	private volatile State state;
	
	/**
	 * Creates a default whisper full params with the recommended settings.
	 * 
	 * @return {@link WhisperFullParams} instance
	 */
	public static WhisperFullParams createWhisperFullParams()
	{
		var params = new WhisperFullParams();
		params.singleSegment = true;
		params.printProgress = false;
		params.printTimestamps = false;
		params.suppressNonSpeechTokens = true;
		params.suppressBlank = true;
		return params;
	}
	
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
	
	private JScribe(Logger logger, WhisperFullParams params, Path modelPath, boolean useVulkan, boolean warmUpModel, boolean noLoadNatives)
	{
		JScribe.logger = logger;
		this.modelPath = modelPath;
		this.params = params;
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
	 * @throws IOException if {@link #isInUse()} or something went wrong
	 */
	public synchronized void start() throws IOException
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
			
			transcriber = new Transcriber(logger, modelPath, params, useVulkan, noLoadNatives);
			
			// Report errors to this thread
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
			
			if(transcriber != null)
				transcriber.shutdown();
			
			// Wait to die
			logger.info("Waiting for threads to die (max wait: {}s)", wait.toSeconds());
			
			long millis = wait.toMillis();
			kill(transcriber, millis);
			// if(recorder != null && recorder.getState() != Thread.State.NEW)
			// recorder.join(wait);
			// if(transcriber != null && recorder.getState() != Thread.State.NEW)
			// transcriber.join(wait);
			
			JScribe.logger.info("Stopped JScribe");
		} catch(InterruptedException e)
		{
			if(Optional.ofNullable(transcriber).map(Transcriber::isAlive).orElse(false))
			{
				JScribe.logger.error("Tried to join transcriber but was interrupted");
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
			transcriber.reset();
		}
		else
		{
			logger.warn("Can't reset JScribe. Not running");
		}
	}
	
	/**
	 * Transcribes a raw audio frame. Must match {@link JScribe#FORMAT}!
	 * 
	 * @param rawSamples normalized audio samples
	 */
	public void transcribe(float[] rawSamples)
	{
		transcriber.newRecording(new Recording(rawSamples));
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
		private boolean vulkan, warmUpModel, noLoadNatives;
		
		private WhisperFullParams params = createWhisperFullParams();
		
		/**
		 * Creates a new JScribe Builder instance. All parameters in this constructor are the minimum required parameters to build a simple instance.
		 * 
		 * @param modelPath path to the GGML model
		 */
		public Builder(Path modelPath)
		{
			this.modelPath = modelPath;
		}
		
		// /**
		// * Defines the minimum amount of audio (in milliseconds) the window must fill to before transcribing the entire window.
		// *
		// * <p>
		// * To only transcribe a full window, set <code>minWindowContext</code> to match the latency.
		// * </p>
		// *
		// * @param minWindowContext
		// * @return
		// */
		// public Builder minimumWindowContext(long minWindowContext)
		// {
		// if(minWindowContext < latency)
		// throw new IllegalArgumentException("Minimum window context must be >= latency");
		//
		// this.minWindowContext = minWindowContext;
		// return this;
		// }
		
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
		 * Sets the whisper params to be used for transcription. By default, uses {@link JScribe#createWhisperFullParams()}.
		 * 
		 * @param params {@link WhisperFullParams} params
		 * @return this, for chaining
		 */
		public Builder setWhisperFullParams(WhisperFullParams params)
		{
			this.params = params;
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
			if(!LibraryUtils.canUseVulkan(JScribe.logger))
			{
				logger.error("Can't use Vulkan, wrong platform / arch ({}, {})", LibraryUtils.OS_NAME, LibraryUtils.OS_ARCH);
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
			return new JScribe(logger, params, modelPath, vulkan, warmUpModel, noLoadNatives);
		}
	}
	
	private static enum State
	{
		INITIALIZING, RUNNING, STOPPING;
	}
	
	/**
	 * Converts an array of 16-bit PCM audio samples into a normalized float array.
	 * 
	 * <p>
	 * Each sample in {@code rawSamples} is a signed 16-bit value (range: -32768 to 32767) and is converted to a float value in the range of [-1.0, 1.0]. This
	 * format is commonly used for audio processing libraries that operate on normalized float values.
	 * </p>
	 *
	 * @param rawSamples the raw 16-bit PCM audio samples to convert
	 * @return a float array of normalized audio samples in the range [-1.0, 1.0]
	 */
	public static float[] pcmToFloat(short[] rawSamples)
	{
		// Normalize the 16-bit short values to float values between -1 and 1
		float[] samples = new float[rawSamples.length];
		
		for(int i = 0; i < samples.length; i++)
		{
			// Normalize the 16-bit short value to a float between -1 and 1
			float newVal = Math.max(-1f, Math.min(((float) rawSamples[i]) / 32768f, 1f));
			samples[i++] = newVal;
		}
		
		return samples;
	}
	
	/**
	 * Converts a normalized float array into 16-bit PCM audio samples.
	 * 
	 * @param buffer normalized float array
	 * @return 16-bit PCM audio samples
	 */
	public static byte[] floatToPCM(float[] buffer)
	{
		// Convert float[] to 16-bit little-endian PCM bytes
		byte[] pcmData = new byte[buffer.length * 2];
		
		for(int i = 0; i < buffer.length; i++)
		{
			short pcm = (short) Math.max(Short.MIN_VALUE, Math.min(buffer[i] * Short.MAX_VALUE, Short.MAX_VALUE));
			pcmData[i * 2] = (byte) (pcm & 0xFF); // Low byte
			pcmData[i * 2 + 1] = (byte) ((pcm >> 8) & 0xFF); // High byte
		}
		
		return pcmData;
	}
	
	/**
	 * Writes float[] to a WAV file.
	 * 
	 * @param sampleRate sample rate of the array
	 * @param buffer     PCM samples
	 * @param path       destination path
	 * @throws IOException if something goes wrong
	 */
	public static void writeWavFile(int sampleRate, float[] buffer, Path path) throws IOException
	{
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(floatToPCM(buffer));
		AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false); // little-endian PCM
		
		AudioInputStream audioInputStream = new AudioInputStream(byteArrayInputStream, format, buffer.length);
		
		AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, path.toFile());
		JScribe.logger.info("WAV file written to {}", path.toAbsolutePath());
	}
	
	/**
	 * Reads an audio input stream into a normalized float array.
	 * 
	 * @param stream input stream (like {@link FileInputStream} if reading from disk)
	 * @return normalized float array
	 * @throws IOException                   if something went wrong
	 * @throws UnsupportedAudioFileException if the input stream doesn't represent a proper audio file
	 */
	public static float[] readWavToFloatSamples(InputStream stream) throws IOException, UnsupportedAudioFileException
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
