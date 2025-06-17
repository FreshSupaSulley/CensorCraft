package io.github.freshsupasulley;

import java.io.IOException;
import java.nio.file.Path;

import be.tarsos.dsp.resample.RateTransposer;

public class RollingAudioBuffer {
	
	private final float[] buffer;
	private final int capacity;
	private int writeIndex = 0;
	private boolean filled;
	
	private final RateTransposer transposer;
	
	/**
	 * Initializes a new rolling audio buffer.
	 * 
	 * @param maxBufferMs maximum size (in milliseconds) that the audio buffer can store
	 * @param inputSampleRate  input sample rate
	 */
	public RollingAudioBuffer(int maxBufferMs, int inputSampleRate)
	{
		this.capacity = (int) (maxBufferMs * JScribe.FORMAT.getSampleRate()) / 1000;
		this.buffer = new float[capacity];
		
		transposer = new RateTransposer(JScribe.FORMAT.getSampleRate() * 1f / inputSampleRate);
	}
	
	/**
	 * Appends new samples, overwriting the oldest if the buffer is full.
	 */
	public void append(short[] rawSamples)
	{
		float[] normalized = JScribe.pcmToFloat(rawSamples);
		
//		AudioEvent audioEvent = new AudioEvent(JScribe.FORMAT);
//		audioEvent.setFloatBuffer(normalized);
		
		for(float sample : transposer.process(normalized))
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
		JScribe.writeWavFile((int) JScribe.FORMAT.getSampleRate(), getSnapshot(), path);
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