package com.supasulley.censorcraft.config.punishments;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

public class Kill extends PunishmentOption {
	
	private static ConfigValue<Boolean> IGNORE_TOTEMS;
	
	@Override
	public void build(Builder builder)
	{
		// General settings
		IGNORE_TOTEMS = builder.comment("Killing the player ignores totems (enable must be true)").define("ignore_totem", false);
	}
	
	@Override
	public void punish(ServerPlayer player)
	{
		// If we should ignore totems
		if(IGNORE_TOTEMS.get())
		{
			// Generic kill ignores totems
			player.kill(player.serverLevel());
		}
		else
		{
			// Generic will stop at totems
			player.hurtServer(player.serverLevel(), player.level().damageSources().generic(), Float.MAX_VALUE);
		}
	}
}