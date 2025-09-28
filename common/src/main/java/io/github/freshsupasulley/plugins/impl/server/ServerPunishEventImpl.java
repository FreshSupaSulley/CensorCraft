package io.github.freshsupasulley.plugins.impl.server;

import io.github.freshsupasulley.censorcraft.api.events.server.ServerPunishEvent;
import io.github.freshsupasulley.censorcraft.api.punishments.ServerPunishment;

import java.util.UUID;

public class ServerPunishEventImpl extends ServerEventImpl implements ServerPunishEvent {
	
	private final UUID playerUUID;
	private final ServerPunishment punishment;
	
	public ServerPunishEventImpl(UUID playerUUID, ServerPunishment punishment)
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
	public ServerPunishment getPunishments()
	{
		return punishment;
	}
}
