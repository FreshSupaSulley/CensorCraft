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

/**
 * The entry point of the JScribe library.
 */
public class JScribe implements UncaughtExceptionHandler {
	
	static Logger logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
	
	private boolean useVulkan, warmUpModel;
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
	
	private JScribe(Logger logger, boolean useVulkan, boolean warmUpModel)
	{
		JScribe.logger = logger;
		this.useVulkan = useVulkan;
		this.warmUpModel = warmUpModel;
	}
	
	//
	// /**
	// * Gets the model provided in the constructor.
	// *
	// * @return model path
	// */
	// public Path getModel()
	// {
	// return modelPath;
	// }
	//
	/**
	 * Starts live audio transcription.
	 * 
	 * <p>
	 * You cannot run multiple instances at a time. Use {@linkplain #isInUse()} before to ensure JScribe is ready to start.
	 * </p>
	 * 
	 * @param modelPath  path to GGML formatted Whisper model
	 * @param microphone preferred microphone name (can be null). Use {@link JScribe#getMicrophones()} to get microphones that support the required audio format.
	 * @param latency    audio sample length in milliseconds. Must be at least 30ms. If low, set overlap to be much higher to catch full words.
	 * @param overlap    extra audio in milliseconds. Samples are collected into a window of this size for context to the transcriber. Must be non-negative.
	 * @param vad        voice activity detection. Enabling will only transcribe words when voice activity is detected
	 * @param denoise    true to denoise audio samples to help reduce white noise, false for raw audio
	 * @throws IllegalArgumentException {@code latency < 30} or {@code overlap < 0}
	 * @throws NoMicrophoneException    if no usable microphones were found
	 * @throws IOException              if {@link #isInUse()} or something went wrong
	 */
	public synchronized void start(Path modelPath, String microphone, long latency, long overlap, boolean vad, boolean denoise) throws IOException, NoMicrophoneException
	{
		// Ensure only one process can use JScribe at a time (otherwise causes JNI crashes). Maybe this crash is on a per-model basis, unsure...
		// Null state means not started or stopped
		if(isInUse())
		{
			throw new IOException("JScribe already started. Wait for it to die");
		}
		
		try
		{
			if(latency < 30)
			{
				throw new IllegalArgumentException("Latency must be at least 30ms");
			}
			
			if(overlap < 0)
			{
				throw new IllegalArgumentException("Overlap must be non-negative");
			}
			
			state = State.INITIALIZING;
			
			logger.info("Starting JScribe");
			
			recorder = new AudioRecorder(transcriber = new Transcriber(modelPath, useVulkan), microphone, latency, overlap, true, vad, denoise);
			
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
				synchronized(this)
				{
					// If the transcriber somehow died
					if(!transcriber.isRunning())
					{
						logger.warn("JScribe was stopped before warm-up completed");
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
		if(isShuttingDown())
		{
			return;
		}
		
		try
		{
			logger.info("Stopping JScribe");
			state = State.STOPPING;
			
			if(recorder != null && recorder.isAlive())
				recorder.shutdown();
			if(transcriber != null && transcriber.isAlive())
				transcriber.shutdown();
			
			// Wait to die
			logger.info("Waiting for threads to die (max wait: {}s)", wait.toSeconds());
			
			if(recorder != null && recorder.isAlive())
				recorder.join(wait);
			if(transcriber != null && transcriber.isAlive())
				transcriber.join(wait);
			
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
			transcriber.clearRecordings();
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
	
	/**
	 * Returns true if no audio is being received, meaning there's an open stream to a microphone but it's not returning any data. This can happen when running in
	 * the Eclipse IDE on macOS due to permission problems. Ensure this is running with {@link JScribe#isRunning()} first, otherwise it will always return true.
	 * 
	 * @return true if we're not receiving any audio data from the client, or JScribe is not running
	 */
	public boolean noAudio()
	{
		return !isRunning() || !recorder.receivingAudio();
	}
	
	/**
	 * True if voice is detected, false otherwise. VAD must be enabled in {@link JScribe#start}.
	 * 
	 * @return true if VAD is enabled and it detected the user is speaking
	 */
	public boolean voiceDetected()
	{
		return isRunning() && recorder.voiceDetected();
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
		
		private boolean vulkan, warmUpModel;
		
		/**
		 * Sets the logger all JScribe operations will use.
		 * 
		 * @param logger {@link Logger} instance
		 * @return this, for chaining
		 */
		public Builder setLogger(Logger logger)
		{
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
			return new JScribe(logger, vulkan, warmUpModel);
		}
	}
	
	private static enum State
	{
		INITIALIZING, RUNNING, STOPPING;
	}
	
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
