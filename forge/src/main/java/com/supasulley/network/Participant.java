package com.supasulley.network;

public class Participant {
	
	private String name;
	private long lastHeartbeat = System.currentTimeMillis(), lastPunishment = System.currentTimeMillis();
	
	// Hold 200 characters
	private static final int BUFFER_SIZE = 200;
	private StringBuilder buffer = new StringBuilder(BUFFER_SIZE);
	
	public Participant(String name)
	{
		this.name = name;
	}
	
	public String appendWord(String word)
	{
		// Separate words with spaces
		String string = word + " ";
		
		if(string.length() >= BUFFER_SIZE)
		{
			buffer.setLength(0);
			buffer.append(string.substring(string.length() - BUFFER_SIZE));
		}
		else
		{
			int overflow = buffer.length() + string.length() - BUFFER_SIZE;
			
			if(overflow > 0)
			{
				buffer.delete(0, overflow);
			}
			
			buffer.append(string);
		}
		
		return buffer.toString();
	}
	
	public void clearBuffer()
	{
		buffer.setLength(0);
	}
	
	public void updateLastPunishment()
	{
		this.lastPunishment = System.currentTimeMillis();
	}
	
	public void heartbeat()
	{
		this.lastHeartbeat = System.currentTimeMillis();
	}
	
	public String getName()
	{
		return name;
	}
	
	public long getLastHeartbeat()
	{
		return lastHeartbeat;
	}
	
	public long getLastPunishment()
	{
		return lastPunishment;
	}
}
