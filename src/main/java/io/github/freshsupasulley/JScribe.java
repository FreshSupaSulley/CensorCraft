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
	 * @param microphone preferred microphone name (can be blank)
	 * @param latency    audio sample length in milliseconds
	 * @param overlap    extra audio in milliseconds to catch stray words
	 * @param denoise    true to denoise audio samples, false for raw audio
	 * @return true if transcription started, false otherwise
	 */
	public boolean start(String microphone, long latency, long overlap, boolean denoise)
	{
		if(isRunning())
		{
			return false;
		}
		
		if(latency <= 0)
		{
			throw new IllegalStateException("JScribe is misconfigured");
		}
		
		logger.info("Starting JScribe");
		
		// where do i put running = true lol
		try
		{
			recorder = new AudioRecorder(transcriber = new Transcriber(modelPath), microphone, latency, overlap, denoise);
			
			// Report errors to this thread
			recorder.setUncaughtExceptionHandler(this);
			transcriber.setUncaughtExceptionHandler(this);
			
			recorder.start();
			transcriber.start();
			
			return true;
		} catch(IOException e)
		{
			logger.error("Failed to start", e);
			return false;
		}
	}
	
	/**
	 * Stops and waits for live audio transcription to be stopped.
	 * 
	 * @return true if stopped, false if wasn't running
	 */
	public boolean stop()
	{
		if(!isRunning())
		{
			return false;
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
		return true;
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
	 * Gets all transcribed words and clears the buffer.
	 * 
	 * @return buffer of transcribed words, or empty string
	 */
	public String getBuffer()
	{
		return transcriber.getBuffer();
	}
	
	@Override
	public void uncaughtException(Thread t, Throwable e)
	{
		JScribe.logger.error("JScribe ended early due to an unhandled error in thread " + t.getName(), e);
		stop();
	}
}
