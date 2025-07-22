package io.github.freshsupasulley.plugins.impl.server;

import io.github.freshsupasulley.censorcraft.api.events.server.PunishEvent;
import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;

public class PunishEventImpl extends ServerEventImpl implements PunishEvent {
	
	private final Punishment punishments;
	
	public PunishEventImpl(Punishment punishments)
	{
		this.punishments = punishments;
	}
	
	@Override
	public Punishment getPunishments()
	{
		return punishments;
	}
}
