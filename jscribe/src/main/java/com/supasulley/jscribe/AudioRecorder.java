package com.supasulley.jscribe;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

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
	public static final AudioFormat FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000, 16, 1, 2, 16000, false);
	
	private final Transcriber transcription;
	private final long windowSize;
	
	/** Can change based on transcription speed */
	private long latency;
	
	private volatile boolean running = true, receivingAudio = true;
	
	private Mixer.Info device;
	private float loudness;
	
	public AudioRecorder(Transcriber listener, String micName, long windowSize, long latency)
	{
		this.transcription = listener;
		this.windowSize = windowSize;
		this.latency = latency;
		
		List<Mixer.Info> microphones = getMicrophones();
		
		if(microphones.isEmpty())
		{
			throw new IllegalStateException("No microphones detected");
		}
		
		this.device = microphones.stream().filter(mic -> mic.getName().equals(micName)).findFirst().orElse(microphones.getFirst());
		JScribe.logger.info("Using microphone " + device.getName());
		
		setName("Audio Recorder");
		setDaemon(true);
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
			DataLine.Info lineInfo = new DataLine.Info(TargetDataLine.class, FORMAT);
			
			if(mixer.isLineSupported(lineInfo))
			{
				names.add(mixerInfo);
			}
		}
		
		return names;
	}
	
	public boolean receivingAudio()
	{
		return receivingAudio;
	}
	
	public Mixer.Info getMicrophoneInfo()
	{
		return device;
	}
	
	public long getTranscriptionTime()
	{
		return latency;
	}
	
	public void shutdown()
	{
		running = false;
		interrupt();
	}
	
	public float getAudioLevel()
	{
		return loudness;
	}
	
	@Override
	public void run()
	{
		float[] window = null;
		
		try(TargetDataLine line = AudioSystem.getTargetDataLine(FORMAT, device); AudioInputStream stream = new AudioInputStream(line))
		{
			// checks if system supports the data line
			if(!AudioSystem.isLineSupported(line.getLineInfo()))
			{
				JScribe.logger.error("Line not supported: {}", line.getLineInfo());
				shutdown();
			}
			
			line.open();
			line.start();
			
			while(running)
			{
				try
				{
					float[] rawSamples = recordSample(stream);
					
					if(rawSamples == null)
					{
						JScribe.logger.warn("No audio data");
						receivingAudio = false;
						window = null;
						continue;
					}
					
					receivingAudio = true;
					
					// If we have a window
					if(window != null)
					{
						// If the current window doesn't have enough samples for the full window size yet
						if(window.length < (int) (FORMAT.getSampleRate() * (windowSize / 1000f)))
						{
							JScribe.logger.trace("Expanding window");
							
							// Update window to include this new sample
							float[] newWindow = new float[window.length + rawSamples.length];
							
							// Prepend newWindow with old
							System.arraycopy(window, 0, newWindow, 0, window.length);
							
							window = newWindow;
						}
						// Shift window
						else
						{
							JScribe.logger.trace("Shifting window");
							
							// Shift window to the left to make room for new samples
							float[] newWindow = new float[window.length];
							
							// Copy window with offset
							System.arraycopy(window, rawSamples.length, newWindow, 0, window.length - rawSamples.length);
							
							window = newWindow;
						}
						
						// Append window with new samples
						System.arraycopy(rawSamples, 0, window, window.length - rawSamples.length, rawSamples.length);
					}
					else
					{
						JScribe.logger.trace("No last samples to prepend");
						window = rawSamples;
					}
				} catch(InterruptedException e)
				{
					continue;
				}
				
				// Write to file for testing
				// final float[] samples2 = window;
				// Thread thread = new Thread(() ->
				// {
				// try
				// {
				// writeWavFile(System.currentTimeMillis() + ".wav", samples2, format.getSampleRate(), format.getChannels());
				// } catch(IOException | LineUnavailableException e)
				// {
				// // FIXME Auto-generated catch block
				// e.printStackTrace();
				// }
				// });
				// thread.start();
				
				// Pass samples to transcriber
				transcription.newSample(window);
				
				JScribe.logger.info("{}", transcription.getBacklog());
				
				// long transcribeTime = transcription.getLastTranscriptionTime();
				//
				// // If transcription is taking longer than expected
				// if(transcribeTime > baseLatency)
				// {
				// // If its high but going down
				// /*
				// * if(timeTook < recordTime) { long compromisedTime = (baseRecordTime + timeTook) / 2; JScribe.logger.info("Catching up to base time (" + baseRecordTime +
				// * "ms)! Setting record time to " + compromisedTime + "ms (was " + recordTime + "ms)"); recordTime = compromisedTime; } // It's just going up else
				// */
				// if(transcribeTime > latency)
				// {
				// JScribe.logger.warn("Transcription took {}ms of desired {}ms", transcribeTime, latency);
				// }
				//
				// /*
				// * The priority is to make transcription as live as possible. The more files we have on the transcription backlog, the longer we should record a sample for to
				// * help reduce that backlog. The consequence is transcription becomes more delayed. Do not allow samples too long.
				// */
				// latency = baseLatency + (transcribeTime - baseLatency) * transcription.getBacklog();
				//
				// // 0 indicates no cap
				// if(maxRecordTime != 0 && latency > maxRecordTime)
				// {
				// JScribe.logger.warn("Transcription is taking too long (exceeded {}ms cap)", maxRecordTime);
				// latency = maxRecordTime;
				// }
				// }
			}
		} catch(LineUnavailableException e)
		{
			JScribe.logger.error("Line unavailable", e);
		} catch(IOException e)
		{
			JScribe.logger.error("Something went wrong reading audio input", e);
		}
	}
	
	private static byte[] convertToPCM(float[] samples)
	{
		byte[] pcmData = new byte[samples.length * 2]; // 2 bytes per sample (16-bit)
		for(int i = 0; i < samples.length; i++)
		{
			// Convert the float sample (-1.0 to 1.0) to a 16-bit signed integer
			short pcmSample = (short) (samples[i] * Short.MAX_VALUE);
			pcmData[2 * i] = (byte) (pcmSample & 0xFF);
			pcmData[2 * i + 1] = (byte) ((pcmSample >> 8) & 0xFF);
		}
		return pcmData;
	}
	
	public static void writeWavFile(String filename, float[] samples, float sampleRate, int numChannels) throws IOException, LineUnavailableException
	{
		// Convert the float[] to 16-bit PCM data
		byte[] pcmData = convertToPCM(samples);
		
		// Create a byte array input stream from the PCM data
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(pcmData);
		
		// Define audio format (16-bit PCM, mono/stereo, sample rate)
		AudioFormat format = new AudioFormat(sampleRate, 16, numChannels, true, false); // signed 16-bit PCM, big-endian
		
		// Create an AudioInputStream from the byte array input stream
		AudioInputStream audioInputStream = new AudioInputStream(byteArrayInputStream, format, pcmData.length / format.getFrameSize());
		
		// Write the AudioInputStream to a WAV file using AudioSystem
		File outputFile = new File(filename);
		AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFile);
		
		System.out.println("WAV file has been written to: " + outputFile.getAbsolutePath());
	}
	
	private float[] recordSample(AudioInputStream stream) throws InterruptedException, IOException
	{
		final int singleSampleSize = FORMAT.getSampleSizeInBits() / 8;
		
		// Calcualte how long this sample has to be
		ByteBuffer captureBuffer = ByteBuffer.allocate((int) ((FORMAT.getSampleRate() * (FORMAT.getSampleSizeInBits() / 8) * FORMAT.getChannels() * latency) / 1000f));
		captureBuffer.order(ByteOrder.LITTLE_ENDIAN);
		
		// Read individual samples until assembled
		int bytesRead = 0;
		
		while(bytesRead < captureBuffer.limit())
		{
			// Before we start reading, check if we're running
			// For some reason you can't just rely on Thread.interrupted. Doesn't seem to work all of the time
			if(!running || Thread.interrupted())
			{
				JScribe.logger.info("Audio recorder was interrupted");
				throw new InterruptedException();
			}
			
			byte[] singleSample = new byte[singleSampleSize];
			int read = stream.read(singleSample);
			
			if(read == -1)
			{
				JScribe.logger.error("rip");
				return null;
			}
			
			// Format is little endian
			short sample = (short) ((singleSample[1] << 8) | (singleSample[0] & 0xFF));
			
			// Normalize the sample to [-1, 1]
			float normalizedSample = sample / (float) Short.MAX_VALUE;
			
			// Calculate the dB level for this single sample
			if(normalizedSample == 0)
			{
				// Silence
				loudness = 0;
			}
			else
			{
				// Gives range from (-infinity, 0)
				loudness = 20 * (float) Math.log10(Math.abs(normalizedSample));
				// Transform to 0 to 1
				loudness = Math.clamp(1 - loudness / -100f, 0, 1);
			}
			
			captureBuffer.put(singleSample);
			bytesRead += read;
		}
		
		// Obtain the 16-bit int audio samples (short type in Java)
		var shortBuffer = captureBuffer.flip().asShortBuffer();
		
		// Transform the samples to f32 samples (normalize the values)
		float[] samples = new float[captureBuffer.capacity() / 2]; // Each short is 2 bytes
		int i = 0;
		boolean hasData = false;
		
		while(shortBuffer.hasRemaining())
		{
			// Normalize the 16-bit short value to a float between -1 and 1
			float newVal = Math.max(-1f, Math.min(((float) shortBuffer.get()) / Short.MAX_VALUE, 1f));
			samples[i++] = newVal;
			
			if(!hasData && newVal != 0)
			{
				hasData = true;
			}
		}
		
		if(!hasData)
		{
			JScribe.logger.trace("Audio was recorded, but it seemed to be empty");
			return null;
		}
		
		return samples;
	}
}