package com.supasulley.censorcraft.config.punishments;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

public class Ignite extends PunishmentOption {
	
	private static ConfigValue<Float> IGNITE_SECONDS;
	
	@Override
	public String getDescription()
	{
		return "Puts the player on fire";
	}
	
	@Override
	public void build(Builder builder)
	{
		IGNITE_SECONDS = builder.comment("Amount of seconds player is on fire for").defineInRange("ignite_seconds", 5f, 0, Float.MAX_VALUE);
	}
	
	@Override
	public void punish(ServerPlayer player)
	{
		player.igniteForSeconds(IGNITE_SECONDS.get());
	}
}
