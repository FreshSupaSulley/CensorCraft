package io.github.freshsupasulley.plugins.impl.client;

import io.github.freshsupasulley.censorcraft.api.events.client.ClientPunishEvent;
import io.github.freshsupasulley.censorcraft.api.punishments.ClientPunishment;

public class ClientPunishEventImpl extends ClientEventImpl implements ClientPunishEvent {
	
	private final ClientPunishment punishments;
	
	public ClientPunishEventImpl(ClientPunishment punishments)
	{
		this.punishments = punishments;
	}
	
	@Override
	public ClientPunishment getPunishments()
	{
		return punishments;
	}
}
