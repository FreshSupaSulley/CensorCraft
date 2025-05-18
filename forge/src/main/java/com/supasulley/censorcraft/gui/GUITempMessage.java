package com.supasulley.censorcraft.gui;

public class GUITempMessage {
	
	private String message;
	private long expiryTime;
	
	public GUITempMessage(String message, long lifetimeMs)
	{
		this.message = message;
		this.expiryTime = System.currentTimeMillis() + lifetimeMs;
	}
	
	public String getMessage()
	{
		return message;
	}
	
	/**
	 * @return true if this message has expired and should be removed
	 */
	public boolean isExpired()
	{
		return System.currentTimeMillis() > expiryTime;
	}
}
