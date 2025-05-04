package io.github.freshsupasulley;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.file.Path;

import javax.sound.sampled.Mixer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JScribe implements UncaughtExceptionHandler {
	
	protected static Logger logger;
	
	private final Path modelPath;
	private AudioRecorder recorder;
	private Transcriber transcriber;
	
	public JScribe(Logger logger, Path modelPath)
	{
		JScribe.logger = logger;
		this.modelPath = modelPath;
	}
	
	public JScribe(Path modelPath)
	{
		this(LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME), modelPath);
	}
	
	/**
	 * Starts live audio transcription.
	 * 
	 * @param microphone preferred microphone name (can be null)
	 * @param latency    audio sample length in milliseconds. Must be at least 30ms. If low, set overlap to be much higher to catch full words.
	 * @param overlap    extra audio in milliseconds. Samples are collected into a window of this size for context to the transcriber. Must be non-negative.
	 * @param vad        voice activity detection. Enabling will only transcribe words when voice activity is detected
	 * @param denoise    true to denoise audio samples to help reduce white noise, false for raw audio
	 * @throws IllegalStateException {@code latency < 30} or {@code overlap < 0}
	 * @throws IOException           if JScribe failed to start
	 */
	public void start(String microphone, long latency, long overlap, boolean vad, boolean denoise) throws IOException
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
	 * @return true if running, false otherwise (error or simply stopped)
	 */
	public boolean isRunning()
	{
		return transcriber != null && recorder != null && transcriber.isAlive() && recorder.isAlive();
	}
	
	/**
	 * @return true if we're not receiving any audio data from the client, or JScribe is not running
	 */
	public boolean noAudio()
	{
		return !isRunning() || !recorder.receivingAudio();
	}
	
	/**
	 * @return true if VAD is enabled and it detected the user is speaking
	 */
	public boolean voiceDetected()
	{
		return recorder.voiceDetected();
	}
	
	/**
	 * @return audio level of samples, [0 - 1]
	 */
	public float getAudioLevel()
	{
		if(!isRunning())
			return 0;
		return recorder.getAudioLevel();
	}
	
	/**
	 * @return active microphone information, or null if none was found or not running
	 */
	public Mixer.Info getActiveMicrophone()
	{
		if(!isRunning())
			return null;
		
		return recorder.getMicrophoneInfo();
	}
	
	/**
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
