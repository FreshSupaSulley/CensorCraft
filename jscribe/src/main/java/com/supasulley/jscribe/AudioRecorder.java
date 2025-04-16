package com.supasulley.jscribe;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

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
	private final long overlap;
	
	/** Can change based on transcription speed */
	private long latency;
	
	private volatile boolean running = true, receivingAudio = true;
	
	private Mixer.Info device;
	private float loudness;
	
	public AudioRecorder(Transcriber listener, String micName, long latency, long overlap)
	{
		this.transcription = listener;
		this.latency = latency;
		this.overlap = overlap;
		
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
					
					// If we have a window
					if(window != null)
					{
						final int windowLength = (int) (FORMAT.getSampleRate() * ((latency + overlap) / 1000f));
						
						// If the current window doesn't have enough samples for the full window size yet
						if(window.length < windowLength)
						{
							JScribe.logger.trace("Expanding window");
							
							// Create a new window with the old wnidow appended with enough space for the new audio sample
							// If that exceeds the windowLength, cap it
							float[] newWindow = new float[Math.min(windowLength, window.length + rawSamples.length)];
							
							// Prepend newWindow with old IF we have space for it
							System.arraycopy(window, (window.length + rawSamples.length) - newWindow.length, newWindow, 0, newWindow.length - rawSamples.length);
							
							window = newWindow;
						}
						// Shift window
						else
						{
							JScribe.logger.trace("Shifting window");
							
							// Shift window to the left to make room for the new sample
							float[] newWindow = new float[windowLength];
							
							// Copy old window, making enough room for the raw samples at the end
							System.arraycopy(window, rawSamples.length, newWindow, 0, window.length - rawSamples.length);
							
							window = newWindow;
						}
						
						// Append window with new samples at the very end
						System.arraycopy(rawSamples, 0, window, window.length - rawSamples.length, rawSamples.length);
					}
					else
					{
						JScribe.logger.info("No last samples to prepend");
						window = rawSamples;
					}
					
					// Send to transcriber
					transcription.newSample(window);
				} catch(InterruptedException e)
				{
					JScribe.logger.debug("Interrupted", e);
					continue;
				}
				
				/*
				 * The priority is to make transcription as live as possible. The more files we have on the transcription backlog, the longer we should record a sample for to
				 * help reduce that backlog. The consequence is transcription becomes more delayed. Do not allow samples too long.
				 */
//				long newLatency = baseLatency * (transcription.getBacklog() + 1);
//				
//				newLatency = (++sus) * baseLatency;
				
				// Pass samples to transcriber
				// Do we need to make sure window wont change??
//				transcription.newSample(Arrays.copyOf(window, window.length));
				
				// If transcription is taking longer than expected
//				if(newLatency > latency)
//				{
//					JScribe.logger.warn("Backlog of {}. Raising latency from {}ms to {}ms", transcription.getBacklog(), latency, newLatency);
//					latency = newLatency;
//				}
			}
		} catch(LineUnavailableException e)
		{
			JScribe.logger.error("Line unavailable", e);
		} catch(IOException e)
		{
			JScribe.logger.error("Something went wrong reading audio input", e);
		}
	}
	
//	private static byte[] convertToPCM(float[] samples)
//	{
//		byte[] pcmData = new byte[samples.length * 2]; // 2 bytes per sample (16-bit)
//		for(int i = 0; i < samples.length; i++)
//		{
//			// Convert the float sample (-1.0 to 1.0) to a 16-bit signed integer
//			short pcmSample = (short) (samples[i] * Short.MAX_VALUE);
//			pcmData[2 * i] = (byte) (pcmSample & 0xFF);
//			pcmData[2 * i + 1] = (byte) ((pcmSample >> 8) & 0xFF);
//		}
//		return pcmData;
//	}
//	
//	public static void writeWavFile(String filename, float[] samples, float sampleRate, int numChannels) throws IOException, LineUnavailableException
//	{
//		// Convert the float[] to 16-bit PCM data
//		byte[] pcmData = convertToPCM(samples);
//		
//		// Create a byte array input stream from the PCM data
//		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(pcmData);
//		
//		// Define audio format (16-bit PCM, mono/stereo, sample rate)
//		AudioFormat format = new AudioFormat(sampleRate, 16, numChannels, true, false); // signed 16-bit PCM, big-endian
//		
//		// Create an AudioInputStream from the byte array input stream
//		AudioInputStream audioInputStream = new AudioInputStream(byteArrayInputStream, format, pcmData.length / format.getFrameSize());
//		
//		// Write the AudioInputStream to a WAV file using AudioSystem
//		File outputFile = new File(filename);
//		AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFile);
//		
//		System.out.println("WAV file has been written to: " + outputFile.getAbsolutePath());
//	}
	
	private float[] recordSample(AudioInputStream stream) throws InterruptedException, IOException
	{
		final int sampleSize = 2048 * (FORMAT.getSampleSizeInBits() / 8);
		
		// Calculate how many bytes an audio sample of length latency should have
		ByteBuffer captureBuffer = ByteBuffer.allocate((int) (FORMAT.getSampleRate() * (FORMAT.getSampleSizeInBits() / 8) * FORMAT.getChannels() * (latency / 1000f)));
		captureBuffer.order(ByteOrder.LITTLE_ENDIAN);
		
		// Read individual samples until assembled
		int bytesRead = 0;
		
		boolean hasData = false;
		
		while(bytesRead < captureBuffer.limit())
		{
			// Before we start reading, check if we're running
			// For some reason you can't just rely on Thread.interrupted. Doesn't seem to work all of the time
			if(!running || Thread.interrupted())
			{
				throw new InterruptedException("Audio recorder was interrupted");
			}
			
			byte[] singleSample = new byte[Math.min(sampleSize, captureBuffer.remaining())];
			int read = stream.read(singleSample);
			
			if(read == -1)
			{
				JScribe.logger.error("End of audio stream");
				return null;
			}
			
			// Calculate RMS
			long sum = 0;
			
			// For each short
			for(int i = 0; i < singleSample.length; i += 2)
			{
				// Little endian
				short sample = (short) ((singleSample[i + 1] << 8) | (singleSample[i] & 0xFF));
				sum += sample * sample;
			}
			
			// Immediately update receiving audio rather than waiting for the end of the sample for analysis
			if(!hasData && sum != 0)
			{
				hasData = true;
				receivingAudio = true;
			}
			
			double rms = Math.sqrt(sum / (singleSample.length / 2));
			// number * Math.log10
			// number is the magic here
			// The higher the number, the less sensitive to lower values?
			loudness = 1 - Math.clamp((float) (30 * Math.log10(rms / 32768)) / -100, 0, 1);
			
			captureBuffer.put(singleSample);
			bytesRead += read;
		}
		
		// Before we analyze the samples, check if the audio isn't empty
		if(!hasData)
		{
			JScribe.logger.trace("Audio was recorded, but it seemed to be empty");
			return null;
		}
		
		// Obtain the 16-bit int audio samples (short type in Java)
		var shortBuffer = captureBuffer.flip().asShortBuffer();
		
		// Transform the samples to f32 samples (normalize the values)
		float[] samples = new float[captureBuffer.capacity() / 2]; // Each short is 2 bytes
		int i = 0;
		
		while(shortBuffer.hasRemaining())
		{
			// Normalize the 16-bit short value to a float between -1 and 1
			float newVal = Math.max(-1f, Math.min(((float) shortBuffer.get()) / Short.MAX_VALUE, 1f));
			samples[i++] = newVal;
		}
		
		return samples;
	}
}