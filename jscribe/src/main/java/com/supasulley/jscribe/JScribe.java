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
	private boolean running;
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
	 * @param microphone preferred microphone name
	 * 
	 * @return true if transcription started, false if it was already running
	 */
	public boolean start(String microphone)
	{
		if(running)
		{
			return false;
		}
		
		logger.info("Starting JScribe");
		
		// where do i put running = true lol
		try
		{
			recorder = new AudioRecorder(transcriber = new Transcriber(modelPath), microphone, 2000, 500);
		} catch(IOException e)
		{
			uncaughtException(Thread.currentThread(), e);
			e.printStackTrace();
		}
		
		// Report errors to this thread
		recorder.setUncaughtExceptionHandler(this);
		transcriber.setUncaughtExceptionHandler(this);
		
		running = true;
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
		if(!running)
		{
			return false;
		}
		
		logger.info("Stopping JScribe");
		
		transcriber.shutdown();
		recorder.shutdown();
		
		// Wait to die
		try
		{
			transcriber.join();
			recorder.join();
		} catch(InterruptedException e)
		{
			JScribe.logger.error("Failed to join JScribe threads", e);
			e.printStackTrace();
		}
		
		JScribe.logger.info("Stopped JScribe");
		running = false;
		return true;
	}
	
	/**
	 * @return active microphone information, or null if none was found or not running
	 */
	public Mixer.Info getActiveMicrophone()
	{
		if(!running)
			return null;
		
		return recorder.getMicrophoneInfo();
	}
	
	/**
	 * @return true if running, false otherwise (error or simply stopped)
	 */
	public boolean isRunning()
	{
		return running;
	}
	
	/**
	 * @return true if receiving audio data from the client, false otherwise
	 */
	public boolean isRunningAndNoAudio()
	{
		return running && !transcriber.receivingAudio();
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
