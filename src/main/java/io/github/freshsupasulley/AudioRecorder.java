package io.github.freshsupasulley;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

import de.maxhenkel.rnnoise4j.Denoiser;
import de.maxhenkel.rnnoise4j.UnknownPlatformException;
import io.github.freshsupasulley.Transcriber.Recording;
import io.github.givimad.libfvadjni.VoiceActivityDetector;

class AudioRecorder extends Thread implements Runnable {
	
	/** Format Whisper wants (also means wave file) */
	public static final AudioFormat FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000, 16, 1, 2, 16000, false);
	
	private final Transcriber transcriber;
	private final long overlap;
	private final long latency;
	
	// Debug
	private long lastTimestamp = System.currentTimeMillis();
	
	// Optional things
	private VoiceActivityDetector vad;
	private Denoiser denoiser;
	private boolean padAudio;
	
	private volatile boolean running = true, receivingAudio = true, cleared, voiceDetected;
	
	private Mixer.Info device;
	private float loudness;
	
	public AudioRecorder(Transcriber listener, String micName, long latency, long overlap, boolean padAudio, boolean vad, boolean denoise) throws IOException, NoMicrophoneException
	{
		this.transcriber = listener;
		this.latency = latency;
		this.overlap = overlap;
		this.padAudio = padAudio;
		
		if(vad)
		{
			VoiceActivityDetector.loadLibrary();
			this.vad = VoiceActivityDetector.newInstance();
			this.vad.setMode(VoiceActivityDetector.Mode.QUALITY);
			this.vad.setSampleRate(VoiceActivityDetector.SampleRate.fromValue((int) FORMAT.getSampleRate()));
		}
		
		if(denoise)
		{
			try
			{
				this.denoiser = new Denoiser();
			} catch(UnknownPlatformException e)
			{
				throw new IOException(e);
			}
		}
		
		List<Mixer.Info> microphones = JScribe.getMicrophones();
		
		if(microphones.isEmpty())
		{
			throw new NoMicrophoneException("No microphones detected");
		}
		
		// Objects.equals allows micName to be null
		this.device = microphones.stream().filter(mic -> Objects.equals(mic.getName(), micName)).findFirst().orElse(microphones.getFirst());
		JScribe.logger.info("Using microphone " + device.getName());
		
		setName("Audio Recorder");
		setDaemon(true);
	}
	
	public boolean receivingAudio()
	{
		return receivingAudio;
	}
	
	public boolean voiceDetected()
	{
		return voiceDetected;
	}
	
	public Mixer.Info getMicrophoneInfo()
	{
		return device;
	}
	
	/**
	 * Stops this audio recording for a new one.
	 */
	public void clear()
	{
		cleared = true;
		interrupt();
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
	
	public long getLastTimestamp()
	{
		return lastTimestamp;
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
			
			// Don't close the line??
			line.open();
			line.start();
			
			while(running)
			{
				try
				{
					lastTimestamp = System.currentTimeMillis();
					float[] rawSamples = recordSample(stream);
					
					if(rawSamples == null)
					{
						window = null;
						continue;
					}
					else if(rawSamples.length == 0)
					{
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
						JScribe.logger.trace("No last samples to prepend");
						window = rawSamples;
					}
					
					if(padAudio)
					{
						// Pad to 1000ms (refer to whisper.cpp in whisper in whisper-jni 1.7.1 [whisper btw])
						final int minLength = (int) (FORMAT.getSampleRate() * (1050f / 1000)); // because x1f somehow got 990
						
						if(window.length < minLength)
						{
							JScribe.logger.trace("Padding window with {} zeros", (minLength - window.length));
							
							float[] paddedWindow = new float[minLength];
							System.arraycopy(window, 0, paddedWindow, 0, window.length);
							window = paddedWindow;
						}
					}
					
					if(!cleared)
					{
						// Send to transcriber
						if(transcriber.isAlive())
						{
							transcriber.newRecording(new Recording(window));
						}
					}
				} catch(InterruptedException e)
				{
					// Shutting down should set running to false
					if(running && !cleared)
					{
						JScribe.logger.warn("Interrupted but still running?", e);
					}
					
					continue;
				} finally
				{
					if(cleared)
					{
						JScribe.logger.debug("Clearing window");
						window = null;
					}
					
					cleared = false;
				}
				
				/*
				 * The priority is to make transcription as live as possible. The more files we have on the transcription backlog, the longer we should record a sample for to
				 * help reduce that backlog. The consequence is transcription becomes more delayed. Do not allow samples too long.
				 */
				// long newLatency = baseLatency * (transcription.getBacklog() + 1);
				//
				// newLatency = (++sus) * baseLatency;
				
				// Pass samples to transcriber
				// Do we need to make sure window wont change??
				// transcription.newSample(Arrays.copyOf(window, window.length));
				
				// If transcription is taking longer than expected
				// if(newLatency > latency)
				// {
				// JScribe.logger.warn("Backlog of {}. Raising latency from {}ms to {}ms", transcription.getBacklog(), latency, newLatency);
				// latency = newLatency;
				// }
			}
		} catch(LineUnavailableException e)
		{
			JScribe.logger.error("Line unavailable", e);
			throw new RuntimeException(e);
		} catch(IOException e)
		{
			JScribe.logger.error("Something went wrong reading audio input", e);
		} finally
		{
			if(vad != null)
			{
				vad.close();
			}
			
			if(denoiser != null)
			{
				denoiser.close();
			}
		}
	}
	
	// private static byte[] convertToPCM(float[] samples)
	// {
	// byte[] pcmData = new byte[samples.length * 2]; // 2 bytes per sample (16-bit)
	// for(int i = 0; i < samples.length; i++)
	// {
	// // Convert the float sample (-1.0 to 1.0) to a 16-bit signed integer
	// short pcmSample = (short) (samples[i] * Short.MAX_VALUE);
	// pcmData[2 * i] = (byte) (pcmSample & 0xFF);
	// pcmData[2 * i + 1] = (byte) ((pcmSample >> 8) & 0xFF);
	// }
	// return pcmData;
	// }
	//
	// public static void writeWavFile(String filename, float[] samples, float sampleRate, int numChannels) throws IOException, LineUnavailableException
	// {
	// // Convert the float[] to 16-bit PCM data
	// byte[] pcmData = convertToPCM(samples);
	//
	// // Create a byte array input stream from the PCM data
	// ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(pcmData);
	//
	// // Define audio format (16-bit PCM, mono/stereo, sample rate)
	// AudioFormat format = new AudioFormat(sampleRate, 16, numChannels, true, false); // signed 16-bit PCM, big-endian
	//
	// // Create an AudioInputStream from the byte array input stream
	// AudioInputStream audioInputStream = new AudioInputStream(byteArrayInputStream, format, pcmData.length / format.getFrameSize());
	//
	// // Write the AudioInputStream to a WAV file using AudioSystem
	// File outputFile = new File(filename);
	// AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFile);
	//
	// System.out.println("WAV file has been written to: " + outputFile.getAbsolutePath());
	// }
	
	private float[] recordSample(AudioInputStream stream) throws InterruptedException, IOException
	{
		// Denoiser wants short samples with a length as a multiple of 480 (30ms of audio)
		final int subSamplesSizeInShorts = (int) ((FORMAT.getSampleRate() * FORMAT.getChannels()) / 1000f) * 30;
		
		// Latency is expected to be at least 30ms
		int bufferSizeInShorts = (int) (FORMAT.getSampleRate() * latency * FORMAT.getChannels() * (FORMAT.getSampleSizeInBits() / 8) / 1000f) / 2;
		
		// Round up to the nearest multiple of 480 for denoiser
		ShortBuffer shortBuffer = ShortBuffer.allocate((int) Math.ceil(1f * bufferSizeInShorts / subSamplesSizeInShorts) * subSamplesSizeInShorts);
		
		boolean hasData = false, hasVoiceActivity = vad == null ? true : false;
		final int vadStep = (int) ((FORMAT.getSampleRate() * FORMAT.getChannels()) / 1000f) * 10; // 10ms segments
		
		while(shortBuffer.hasRemaining())
		{
			// Before we start reading, check if we're running
			// For some reason you can't just rely on Thread.interrupted. Doesn't seem to work all of the time
			if(!running || Thread.interrupted())
			{
				throw new InterruptedException("Audio recorder was interrupted");
			}
			
			byte[] subSampleBytes = new byte[subSamplesSizeInShorts * 2]; // convert to bytes
			
			if(stream.read(subSampleBytes) == -1)
			{
				JScribe.logger.error("End of audio stream");
				return null;
			}
			
			// Convert to short array
			short[] shorts = new short[subSampleBytes.length / 2];
			ByteBuffer.wrap(subSampleBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
			
			// Calculate RMS BEFORE denoising so it doesn't show that there's no audio data
			long sum = 0;
			
			for(short toShort : shorts)
			{
				sum += toShort * toShort;
			}
			
			// Immediately update receiving audio rather than waiting for the end of the sample for analysis
			if(!hasData && sum != 0)
			{
				hasData = true;
				receivingAudio = true;
			}
			
			double rms = Math.sqrt(sum / shorts.length);
			// number * Math.log10
			// number is the magic here
			// The higher the number, the less sensitive to lower values?
			loudness = 1 - Math.clamp((float) (20 * Math.log10(rms / 32768)) / -100, 0, 1);
			
			// Denoising
			if(denoiser != null)
			{
				shorts = denoiser.denoise(shorts);
			}
			
			// VAD Processing
			if(vad != null)
			{
				boolean subSampleVoice = false;
				
				// VAD processing with adjusted step size
				// Avoid catching an entire frame of 0 padded frames by stopping at one step away
				for(int i = 0; i < shorts.length - vadStep; i += vadStep)
				{
					short[] frame = Arrays.copyOfRange(shorts, i, i + vadStep);
					
					if(vad.process(frame))
					{
						subSampleVoice = true;
						hasVoiceActivity = true;
						voiceDetected = true;
						break;
					}
				}
				
				// For strictly updating this value live
				if(!subSampleVoice)
				{
					voiceDetected = false;
				}
			}
			
			shortBuffer.put(shorts);
		}
		
		// Before we analyze the samples, check if the audio isn't empty
		if(!hasData)
		{
			JScribe.logger.trace("Audio was recorded, but it seemed to be empty");
			receivingAudio = false;
			return null;
		}
		
		if(!hasVoiceActivity)
		{
			JScribe.logger.trace("No voice activity detected");
			return new float[0];
		}
		
		// Move position back to 0 for normalization
		shortBuffer.flip();
		
		// Normalize the 16-bit short values to float values between -1 and 1
		float[] samples = new float[shortBuffer.remaining()];
		int i = 0;
		
		while(shortBuffer.hasRemaining())
		{
			// Normalize the 16-bit short value to a float between -1 and 1
			float newVal = Math.max(-1f, Math.min(((float) shortBuffer.get()) / 32768f, 1f));
			samples[i++] = newVal;
		}
		
		// Return the normalized float samples
		return samples;
	}
}