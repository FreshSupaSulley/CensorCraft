package io.github.freshsupasulley.censorcraft.jscribe;

import java.io.IOException;
import java.nio.file.Path;

import io.github.freshsupasulley.censorcraft.tarsos.RateTransposer;

/**
 * Converts raw samples into the format whisper expects while maintaining a buffer of previous samples for context.
 */
public class RollingAudioBuffer {
	
	private final float[] buffer;
	private final int capacity;
	private int writeIndex;
	private boolean filled;
	
	// Keeping track of last appended samples
	private int lastAppendStart, lastAppendLength;
	
	private final RateTransposer transposer;
	
	/**
	 * Initializes a new rolling audio buffer.
	 * 
	 * @param maxBufferMs     maximum size (in milliseconds) that the audio buffer can store
	 * @param inputSampleRate input sample rate
	 */
	public RollingAudioBuffer(int maxBufferMs, int inputSampleRate)
	{
		this.capacity = (int) (maxBufferMs * JScribe.FORMAT.getSampleRate()) / 1000;
		this.buffer = new float[capacity];
		
		transposer = new RateTransposer(JScribe.FORMAT.getSampleRate() * 1f / inputSampleRate);
	}
	
	/**
	 * Appends new samples, overwriting the oldest if the buffer is full.
	 * 
	 * @param rawSamples samples to append to the buffer
	 */
	public void append(short[] rawSamples)
	{
		float[] normalized = JScribe.pcmToFloat(rawSamples);
		float[] processed = transposer.process(normalized);
		
		lastAppendStart = writeIndex; // where this append starts
		lastAppendLength = processed.length; // how many samples were appended
		
		for(float sample : processed)
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
	 * Returns a copy of the last samples appended to the audio buffer without clearing.
	 * 
	 * @return copy of the last samples appended
	 */
	public float[] getLastAppended()
	{
		float[] out = new float[lastAppendLength];
		
		if(lastAppendLength == 0)
			return out;
		
		int end = (lastAppendStart + lastAppendLength) % capacity;
		
		if(lastAppendStart < end || !filled)
		{
			// Not filled and thus not wrapped around yet
			System.arraycopy(buffer, lastAppendStart, out, 0, lastAppendLength);
		}
		else
		{
			// We are filled, handle the wrap around
			int firstPart = capacity - lastAppendStart;
			System.arraycopy(buffer, lastAppendStart, out, 0, firstPart);
			System.arraycopy(buffer, 0, out, firstPart, end);
		}
		
		return out;
	}
	
	/**
	 * Returns a copy of the current buffer contents in correct chronological order without clearing the buffer.
	 * 
	 * @return copy of buffer
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
	 * 
	 * @return copy of buffer
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
	
	/**
	 * Determines if this audio buffer is full and will roll over old data when new data is appended.
	 * 
	 * @return true if full
	 */
	public boolean isFull()
	{
		return filled;
	}
	
	/**
	 * Returns the <b>used</b> size of the audio buffer. If full, it will return the capacity.
	 * 
	 * @return currently used size of the audio buffer
	 */
	public int getSize()
	{
		return filled ? capacity : writeIndex;
	}
	
	/**
	 * Returns the total capacity of the audio buffer in number of samples.
	 * 
	 * @return number of samples the audio buffer can store
	 */
	public int getCapacity()
	{
		return capacity;
	}
}