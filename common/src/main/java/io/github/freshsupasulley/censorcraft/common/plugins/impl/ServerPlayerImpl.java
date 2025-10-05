package io.github.freshsupasulley.censorcraft.common.plugins.impl;

import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.ServerPlayer;

import java.util.UUID;

public class ServerPlayerImpl implements ServerPlayer {
	
	private net.minecraft.server.level.ServerPlayer player;
	
	public ServerPlayerImpl(net.minecraft.server.level.ServerPlayer player)
	{
		this.player = player;
	}
	
	@Override
	public ServerLevel getServerLevel()
	{
		return new ServerLevelImpl(player.level());
	}
	
	@Override
	public Object getPlayer()
	{
		return player;
	}
	
	@Override
	public UUID getUuid()
	{
		return player.getUUID();
	}
	
	@Override
	public Object getEntity()
	{
		return player;
	}
	
	@Override
	public Position getPosition()
	{
		return new PositionImpl(player.position());
	}
}
