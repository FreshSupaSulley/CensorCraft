package io.github.freshsupasulley;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The entry point of the JScribe library.
 */
public class JScribe implements UncaughtExceptionHandler {
	
	static Logger logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
	
	private final Path modelPath;
	private AudioRecorder recorder;
	private Transcriber transcriber;
	
	/**
	 * Gets all available Whisper models in GGML format from Hugging Face. Useful to pass into {@link JScribe#downloadModel}.
	 * 
	 * @return array of model names
	 * @throws IOException if something went wrong
	 */
	public static String[] getModels() throws IOException
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
			List<String> models = new ArrayList<String>();
			
			// This only takes proper models (not encoding ones)
			Pattern pattern = Pattern.compile("ggml-(.+?)\\.bin");
			
			for(int i = 0; i < files.length(); i++)
			{
				String fileName = files.getJSONObject(i).getString("path");
				Matcher matcher = pattern.matcher(fileName);
				
				if(matcher.find())
				{
					models.add(matcher.group(1));
				}
			}
			
			return models.toArray(new String[0]);
		} catch(InterruptedException e)
		{
			throw new IOException(e);
		}
	}
	
	/**
	 * Downloads a Whisper model in GGML format from Hugging Face.
	 * 
	 * @param modelName   name of the model (use {@link JScribe#getModels()})
	 * @param destination output path
	 * @throws IOException if something went wrong
	 */
	public static void downloadModel(String modelName, Path destination) throws IOException
	{
		String fileName = "ggml-" + modelName + ".bin";
		String downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/" + fileName;
		
		JScribe.logger.info("Downloading model {} from {} to {}", modelName, downloadUrl, destination.toAbsolutePath());
		
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(downloadUrl)).build();
		
		// It sends you to a unique node so follow the redirect
		try(HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build())
		{
			HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
			
			if(response.statusCode() != 200)
			{
				throw new IOException("Failed to download model. HTTP status code: " + response.statusCode());
			}
			
			try(InputStream in = response.body(); FileOutputStream out = new FileOutputStream(destination.toFile()))
			{
				in.transferTo(out);
			}
			
			JScribe.logger.info("Model saved to {}", destination.toAbsolutePath());
		} catch(InterruptedException e)
		{
			throw new IOException(e);
		}
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
	
	/**
	 * Initializes a new {@link JScribe} instance with a custom logger.
	 * 
	 * @param logger    custom logger
	 * @param modelPath path to GGML formatted Whisper model
	 */
	public JScribe(Logger logger, Path modelPath)
	{
		JScribe.logger = logger;
		this.modelPath = modelPath;
	}
	
	/**
	 * Uses the default root logger.
	 * 
	 * @param modelPath path to GGML formatted Whisper model
	 */
	public JScribe(Path modelPath)
	{
		this(logger, modelPath);
	}
	
	/**
	 * Starts live audio transcription.
	 * 
	 * @param microphone preferred microphone name (can be null). Use {@link JScribe#getMicrophones()} to get microphones that support the required audio format.
	 * @param latency    audio sample length in milliseconds. Must be at least 30ms. If low, set overlap to be much higher to catch full words.
	 * @param overlap    extra audio in milliseconds. Samples are collected into a window of this size for context to the transcriber. Must be non-negative.
	 * @param vad        voice activity detection. Enabling will only transcribe words when voice activity is detected
	 * @param denoise    true to denoise audio samples to help reduce white noise, false for raw audio
	 * @throws IllegalArgumentException {@code latency < 30} or {@code overlap < 0}
	 * @throws NoMicrophoneException    if no usable microphones were found
	 * @throws IOException              if already running or something went wrong
	 */
	public void start(String microphone, long latency, long overlap, boolean vad, boolean denoise) throws IOException, NoMicrophoneException
	{
		if(isRunning())
		{
			throw new IOException("JScribe is already running");
		}
		
		if(latency < 30)
		{
			throw new IllegalArgumentException("Latency must be at least 30ms");
		}
		
		if(overlap < 0)
		{
			throw new IllegalArgumentException("Overlap must be non-negative");
		}
		
		logger.info("Starting JScribe");
		
		recorder = new AudioRecorder(transcriber = new Transcriber(modelPath), microphone, latency, overlap, vad, denoise);
		
		// Report errors to this thread
		recorder.setUncaughtExceptionHandler(this);
		transcriber.setUncaughtExceptionHandler(this);
		
		recorder.start();
		transcriber.start();
	}
	
	/**
	 * Stops and waits for live audio transcription to be stopped.
	 */
	public void stop()
	{
		if(!isRunning())
		{
			return;
		}
		
		logger.info("Stopping JScribe");
		
		recorder.shutdown();
		transcriber.shutdown();
		
		// Wait to die
		try
		{
			recorder.join();
			transcriber.join();
		} catch(InterruptedException e)
		{
			JScribe.logger.error("Failed to join JScribe threads (running: {}, {})", recorder.isAlive(), transcriber.isAlive(), e);
			e.printStackTrace();
		}
		
		JScribe.logger.info("Stopped JScribe");
	}
	
	/**
	 * Returns true if this is running in any capacity is still alive (recording audio, transcribing, etc.).
	 * 
	 * @return true if running, false otherwise (error or simply stopped)
	 */
	public boolean isRunning()
	{
		return transcriber != null && recorder != null && transcriber.isAlive() && recorder.isAlive();
	}
	
	/**
	 * Returns true if no audio is being received, meaning there's an open stream to a microphone but it's not returning any data. This can happen, for example,
	 * when running in some IDEs on macOS due to permission problems. Ensure this is running with {@link JScribe#isRunning()} first, otherwise it will return true
	 * anyways.
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
		return recorder.voiceDetected();
	}
	
	/**
	 * Returns the current volume using RMS.
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
}
