package com.supasulley.jscribe;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

public class AudioRecorder extends Thread implements Runnable {
	
	/** Format Whisper wants (also means wave file) */
	private static final AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000, 16, 1, 2, 16000, false);
	private static final AudioFileFormat.Type FILE_TYPE = AudioFileFormat.Type.WAVE;
	private static final long MAX_RECORD_TIME = 30000;
	
	private final Transcriber transcription;
	private final File parentFolder;
	private final long baseRecordTime, overlapTime;
	private final AudioWriterWorker[] workers;
	
	/** Can change based on transcription speed */
	private long recordTime;
	
	private volatile boolean running = true;
	
	private Mixer.Info device;
	
	public AudioRecorder(Transcriber listener, String micName, long recordTime, long overlapTime) throws IOException
	{
		this.transcription = listener;
		this.workers = new AudioWriterWorker[5];
		this.baseRecordTime = recordTime;
		this.recordTime = recordTime;
		this.overlapTime = overlapTime;
		this.parentFolder = Files.createTempDirectory("samples").toFile();
		this.parentFolder.deleteOnExit();
		
		if(overlapTime >= recordTime)
		{
			throw new IllegalStateException("Overlap time may not be greater than or equal to record time");
		}
		
		List<Mixer.Info> microphones = getMicrophones();
		
		if(microphones.isEmpty())
		{
			throw new IllegalStateException("No microphones detected");
		}
		
		this.device = microphones.stream().filter(mic -> mic.getName().equals(micName)).findFirst().orElse(microphones.getFirst());
		JScribe.logger.info("Using microphone " + device.getName());
	}
	
	/**
	 * @return list of mixers that support the desired {@linkplain AudioFormat} required for transcription.
	 */
	public static List<Mixer.Info> getMicrophones()
	{
		List<Mixer.Info> names = new ArrayList<Mixer.Info>();
		Mixer.Info[] mixers = AudioSystem.getMixerInfo();
		
		for(Mixer.Info mixerInfo : mixers)
		{
			Mixer mixer = AudioSystem.getMixer(mixerInfo);
			DataLine.Info lineInfo = new DataLine.Info(TargetDataLine.class, format);
			
			if(mixer.isLineSupported(lineInfo))
			{
				names.add(mixerInfo);
			}
		}
		
		return names;
	}
	
	public Mixer.Info getMicrophoneInfo()
	{
		return device;
	}
	
	public long getTranscriptionTime()
	{
		return recordTime;
	}
	
	@Override
	public void run()
	{
		long startTime = System.currentTimeMillis();
		newSample();
		
		while(running)
		{
			if(System.currentTimeMillis() - startTime > recordTime - overlapTime)
			{
				long timeTook = transcription.getLastTranscriptionTime();
				
				// If transcription is taking longer than expected
				if(timeTook > baseRecordTime)
				{
					// If its high but going down
					/*
					 * if(timeTook < recordTime) { long compromisedTime = (baseRecordTime + timeTook) / 2; JScribe.logger.info("Catching up to base time (" + baseRecordTime +
					 * "ms)! Setting record time to " + compromisedTime + "ms (was " + recordTime + "ms)"); recordTime = compromisedTime; } // It's just going up else
					 */
					if(timeTook > recordTime)
					{
						JScribe.logger.warn("Transcription took {}ms of desired {}ms", timeTook, recordTime);
					}
					/*
					 * The priority is to make transcription as live as possible. The more files we have on the transcription backlog, the longer we should record a sample for to
					 * help reduce that backlog. The consequence is transcription becomes more delayed. Do not allow samples too long.
					 */
					recordTime = baseRecordTime + (timeTook - baseRecordTime) * transcription.getBacklog();
					
					if(recordTime > MAX_RECORD_TIME)
					{
						JScribe.logger.warn("Transcription is taking too long (exceeded {}ms cap)", MAX_RECORD_TIME);
						recordTime = MAX_RECORD_TIME;
					}
				}
				
				startTime = System.currentTimeMillis();
				newSample();
			}
		}
	}
	
	private void newSample()
	{
		boolean found = false;
		
		// Find open slot
		for(int i = 0; i < workers.length; i++)
		{
			if(workers[i] == null || !workers[i].isAlive())
			{
				found = true;
				workers[i] = new AudioWriterWorker();
				break;
			}
		}
		
		if(!found)
		{
			JScribe.logger.error("Failed to start new AudioWriterWorker. All slots ({}) already running (good sign something is very wrong!)", workers.length);
		}
	}
	
	public void shutdown()
	{
		running = false;
		
		for(int i = 0; i < workers.length; i++)
		{
			if(workers[i] != null)
			{
				workers[i].interrupt();
			}
		}
	}
	
	private class AudioWriterWorker extends Thread implements Runnable {
		
		private File outputFile;
		
		private AudioWriterWorker()
		{
			try
			{
				this.outputFile = File.createTempFile("jscribe", "." + FILE_TYPE.getExtension(), parentFolder);
				this.outputFile.deleteOnExit();
			} catch(IOException e)
			{
				throw new IllegalStateException(e);
			}
			
			setDaemon(true);
			setName("Audio Writer Worker");
			start();
		}
		
		/**
		 * Captures the sound and record into a WAV file
		 */
		@Override
		public void run()
		{
			try(TargetDataLine line = AudioSystem.getTargetDataLine(format, device))
			{
				// checks if system supports the data line
				if(!AudioSystem.isLineSupported(line.getLineInfo()))
				{
					JScribe.logger.error("Line not supported: {}", line.getLineInfo());
					throw new IllegalStateException("Line not supported");
				}
				
				// Ensure parent folders exist
				outputFile.getParentFile().mkdirs();
				
				line.open();
				line.start();
				
				// Create an AudioInputStream from the TargetDataLine
				// Write the audio data in chunks of 4096 bytes
				try(AudioInputStream audioStream = new AudioInputStream(line))
				{
					new Timer().schedule(new TimerTask()
					{
						
						@Override
						public void run()
						{
							line.stop();
							line.close();
						}
					}, recordTime);
					
					AudioSystem.write(audioStream, FILE_TYPE, outputFile);
				} catch(IOException e)
				{
					if(Thread.currentThread().isInterrupted())
					{
						JScribe.logger.warn("Audio Writer Worker was interrupted");
					}
					
					e.printStackTrace();
				} finally
				{
					line.stop();
					line.close();
				}
			} catch(LineUnavailableException e)
			{
				JScribe.logger.error("Failed to record audio to file", e);
			}
			
			JScribe.logger.trace("New audio sample at {}", outputFile);
			
			// Invoke the listener
			transcription.newSample(outputFile);
		}
	}
}