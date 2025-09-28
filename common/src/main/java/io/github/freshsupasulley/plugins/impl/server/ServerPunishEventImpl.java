package io.github.freshsupasulley.plugins.impl.server;

import io.github.freshsupasulley.censorcraft.api.events.server.ServerPunishEvent;
import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;

import java.util.UUID;

public class ServerPunishEventImpl extends ServerEventImpl implements ServerPunishEvent {
	
	private final UUID playerUUID;
	private final Punishment punishment;
	
	public ServerPunishEventImpl(UUID playerUUID, Punishment punishment)
	{
		this.playerUUID = playerUUID;
		this.punishment = punishment;
	}
	
	@Override
	public UUID getPlayerUUID()
	{
		return playerUUID;
	}
	
	@Override
	public Punishment getPunishment()
	{
		return punishment;
	}
}
