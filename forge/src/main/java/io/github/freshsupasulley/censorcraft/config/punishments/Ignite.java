package io.github.freshsupasulley.censorcraft.config.punishments;

import net.minecraft.server.level.ServerPlayer;

public class Ignite extends ForgePunishment {
	
	@Override
	public void buildConfig()
	{
		defineInRange("ignite_seconds", 5D, 0D, Double.MAX_VALUE, "Amount of seconds player is on fire for");
	}
	
	@Override
	public void punish(ServerPlayer player)
	{
		Number ignite = config.get("ignite_seconds");
		player.igniteForSeconds(ignite.floatValue());
	}
}
