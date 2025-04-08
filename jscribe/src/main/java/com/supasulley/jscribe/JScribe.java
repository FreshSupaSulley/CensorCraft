package com.supasulley.jscribe;

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
		
		try
		{
			// Load natives
			Transcriber.loadNatives();
		} catch(IOException e)
		{
			throw new IllegalStateException(e);
		}
	}
	
	public JScribe(Path modelPath)
	{
		this(LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME), modelPath);
	}
	
	/**
	 * Starts live audio transcription.
	 * 
	 * @param microphone  preferred microphone name
	 * @param recordTime  audio sample time in milliseconds (must be greater than 0)
	 * @param overlapTime amount of overlap between samples in milliseconds (must not exceed recordTime)
	 * @param overlapTime amount of overlap between samples in milliseconds (at least recordTime + overlapTime). 0 indicates no max
	 * @return true if transcription started, false otherwise
	 */
	
	/**
	 * Starts live audio transcription.
	 * 
	 * @param microphone preferred microphone name
	 * @param windowSize length of appended audio samples in milliseconds sent to the model
	 * @param latency    interval in milliseconds for when the window is sent to the model
	 * @return true if transcription started, false otherwise
	 */
	public boolean start(String microphone, long windowSize, long latency)
	{
		if(isRunning())
		{
			return false;
		}
		
		if(windowSize <= 0 || latency <= 0 || latency )
		{
			JScribe.logger.error("JScribe is misconfigured");
			return false;
		}
		
		logger.info("Starting JScribe");
		
		// where do i put running = true lol
		recorder = new AudioRecorder(transcriber = new Transcriber(modelPath), microphone, windowSize, latency);
		// Report errors to this thread
		recorder.setUncaughtExceptionHandler(this);
		transcriber.setUncaughtExceptionHandler(this);
		
		recorder.start();
		transcriber.start();
		return true;
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
			JScribe.logger.error("Failed to join JScribe threads", e);
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
	 * @return true if receiving audio data from the client, false otherwise
	 */
	public boolean isRunningAndNoAudio()
	{
		return isRunning() && !recorder.receivingAudio();
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
	 * @return number of queued audio files needed to be transcribed
	 */
	public int getBacklog()
	{
		return transcriber.getBacklog();
	}
	
	/**
	 * Gets all transcribed words and clears the buffer.
	 * 
	 * @return buffer of transcribed words, or empty string
	 */
	public String getBuffer()
	{
		// if(Math.random() < 0.05)
		// {
		// return System.currentTimeMillis() + "";
		// }
		return transcriber.getBuffer();
	}
	
	@Override
	public void uncaughtException(Thread t, Throwable e)
	{
		JScribe.logger.error("JScribe ended early due to an unhandled error in thread " + t.getName(), e);
		stop();
	}
}
