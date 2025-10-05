package io.github.freshsupasulley.censorcraft.common.plugins.impl.server;

import io.github.freshsupasulley.censorcraft.api.events.server.ReceiveTranscription;

import java.util.UUID;

public class ReceiveTranscriptionImpl extends ServerEventImpl implements ReceiveTranscription {
	
	private UUID playerUUID;
	private String text;
	
	public ReceiveTranscriptionImpl(UUID playerUUID, String text)
	{
		this.playerUUID = playerUUID;
		this.text = text;
	}
	
	@Override
	public UUID getPlayerUUID()
	{
		return playerUUID;
	}
	
	@Override
	public String getText()
	{
		return text;
	}
}
