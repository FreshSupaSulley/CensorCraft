package io.github.freshsupasulley;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.resample.RateTransposer;

public class RollingAudioBuffer {
	
	// The format Whisper wants (wave file)
	// public static final AudioFormat FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000, 16, 1, 2, 16000, false);
	public static final TarsosDSPAudioFormat FORMAT = new TarsosDSPAudioFormat(16000, 16, 1, true, false);
	
	private final float[] buffer;
	private final int sampleRate, capacity;
	private int writeIndex = 0;
	private boolean filled;
	
	/**
	 * Initializes a new rolling audio buffer.
	 * 
	 * @param maxBufferMs maximum size (in milliseconds) that the audio buffer can store
	 * @param sampleRate  input sample rate
	 */
	public RollingAudioBuffer(int maxBufferMs, int sampleRate)
	{
		this.sampleRate = sampleRate;
		this.capacity = (int) (maxBufferMs * FORMAT.getSampleRate()) / 1000;
		this.buffer = new float[capacity];
	}
	
	/**
	 * Appends new samples, overwriting the oldest if the buffer is full.
	 */
	public void append(short[] rawSamples)
	{
		float[] normalized = JScribe.normalizePcmToFloat(rawSamples);
		
		RateTransposer resampler = new RateTransposer(FORMAT.getSampleRate() * 1f / sampleRate);
		AudioEvent audioEvent = new AudioEvent(FORMAT);
		audioEvent.setFloatBuffer(normalized);
		resampler.process(audioEvent);
		
		for(float sample : audioEvent.getFloatBuffer())
		{
			buffer[writeIndex] = sample;
			writeIndex = (writeIndex + 1) % capacity;
			
			if(writeIndex == 0)
			{
				filled = true;
			}
		}
	}
	
	/**
	 * Returns a copy of the current buffer contents in correct chronological order without clearing the buffer.
	 */
	public float[] getSnapshot()
	{
		int size = getSize();
		float[] out = new float[size];
		
		if(filled)
		{
			int start = writeIndex;
			int tailLen = capacity - start;
			
			if(size <= tailLen)
			{
				System.arraycopy(buffer, start, out, 0, size);
			}
			else
			{
				System.arraycopy(buffer, start, out, 0, tailLen);
				System.arraycopy(buffer, 0, out, tailLen, size - tailLen);
			}
		}
		else
		{
			System.arraycopy(buffer, 0, out, 0, size);
		}
		
		return out;
	}
	
	/**
	 * Returns a copy of the current buffer contents in correct chronological order, then clears the buffer.
	 */
	public float[] drain()
	{
		if(writeIndex == 0 && !filled)
			return new float[0];
		
		int size = getSize();
		float[] out = new float[size];
		
		if(filled)
		{
			System.arraycopy(buffer, writeIndex, out, 0, capacity - writeIndex);
			System.arraycopy(buffer, 0, out, capacity - writeIndex, writeIndex);
		}
		else
		{
			System.arraycopy(buffer, 0, out, 0, writeIndex);
		}
		
		writeIndex = 0;
		filled = false;
		
		return out;
	}
	
	/**
	 * Writes {@link #getSnapshot()} to a WAV file.
	 * 
	 * @param path destination path
	 * @throws IOException if something goes wrong
	 */
	public void writeWavFile(Path path) throws IOException
	{
		float[] buffer = getSnapshot();
		
		// Convert float[] to 16-bit little-endian PCM bytes
		byte[] pcmData = new byte[buffer.length * 2];
		
		for(int i = 0; i < buffer.length; i++)
		{
			short pcm = (short) Math.max(Short.MIN_VALUE, Math.min(buffer[i] * Short.MAX_VALUE, Short.MAX_VALUE));
			pcmData[i * 2] = (byte) (pcm & 0xFF); // Low byte
			pcmData[i * 2 + 1] = (byte) ((pcm >> 8) & 0xFF); // High byte
		}
		
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(pcmData);
		AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false); // little-endian PCM
		
		AudioInputStream audioInputStream = new AudioInputStream(byteArrayInputStream, format, buffer.length);
		
		AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, path.toFile());
		JScribe.logger.info("WAV file written to {}", path.toAbsolutePath());
	}
	
	public boolean isFull()
	{
		return filled;
	}
	
	public int getSize()
	{
		return filled ? capacity : writeIndex;
	}
	
	public int getCapacity()
	{
		return capacity;
	}
}
