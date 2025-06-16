package io.github.freshsupasulley;

import java.io.IOException;
import java.nio.file.Path;

import be.tarsos.dsp.resample.RateTransposer;

public class RollingAudioBuffer {
	
	private static final int INPUT_SAMPLE_RATE = 48000;
	
	private final float[] buffer;
	private final int capacity;
	private int writeIndex = 0;
	private boolean filled;
	
	/**
	 * Initializes a new rolling audio buffer. The input sample rate is expected to match simple voice chat at 48kHz.
	 * 
	 * @param maxBufferMs maximum size (in milliseconds) that the audio buffer can store
	 */
	public RollingAudioBuffer(int maxBufferMs)
	{
		this.capacity = (int) (maxBufferMs * JScribe.FORMAT.getSampleRate()) / 1000;
		this.buffer = new float[capacity];
	}
	
	/**
	 * Appends new samples, overwriting the oldest if the buffer is full.
	 */
	public void append(short[] rawSamples)
	{
		float[] normalized = JScribe.pcmToFloat(rawSamples);
		
		RateTransposer resampler = new RateTransposer(JScribe.FORMAT.getSampleRate() / INPUT_SAMPLE_RATE);
		float[] resampled = new float[(int) (normalized.length * (JScribe.FORMAT.getSampleRate() / INPUT_SAMPLE_RATE))];
        resampler.process(JScribe.FORMAT.getSampleRate() / INPUT_SAMPLE_RATE, normalized, 0, normalized.length, true, resampled, 0, resampled.length);
		
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
		JScribe.writeWavFile(INPUT_SAMPLE_RATE, getSnapshot(), path);
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
