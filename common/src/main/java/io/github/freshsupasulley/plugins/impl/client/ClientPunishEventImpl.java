package io.github.freshsupasulley.plugins.impl.client;

import io.github.freshsupasulley.censorcraft.api.events.client.ClientPunishEvent;
import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;

public class ClientPunishEventImpl extends ClientEventImpl implements ClientPunishEvent {
	
	private final Punishment punishments;
	
	public ClientPunishEventImpl(Punishment punishments)
	{
		this.punishments = punishments;
	}
	
	@Override
	public Punishment getPunishment()
	{
		return punishments;
	}
}
