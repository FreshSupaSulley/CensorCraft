package io.github.freshsupasulley.plugins.impl.client;

import io.github.freshsupasulley.censorcraft.api.events.client.SendTranscriptionEvent;

public class SendTranscriptionImpl extends ClientEventImpl implements SendTranscriptionEvent {
	
	private final String transcription;
	
	public SendTranscriptionImpl(String transcription)
	{
		this.transcription = transcription;
	}
	
	@Override
	public String getTranscription()
	{
		return transcription;
	}
}
