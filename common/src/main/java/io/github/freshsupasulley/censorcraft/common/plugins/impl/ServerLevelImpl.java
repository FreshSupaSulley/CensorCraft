package io.github.freshsupasulley.censorcraft.common.plugins.impl;

import de.maxhenkel.voicechat.api.ServerLevel;

public class ServerLevelImpl implements ServerLevel {
	
	private net.minecraft.server.level.ServerLevel level;
	
	public ServerLevelImpl(net.minecraft.server.level.ServerLevel level)
	{
		this.level = level;
	}
	
	@Override
	public Object getServerLevel()
	{
		return level;
	}
}
