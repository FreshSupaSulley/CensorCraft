package io.github.freshsupasulley.censorcraft.config.punishments;

import net.minecraft.server.level.ServerPlayer;

public class Kill extends PunishmentOption<Kill> {
	
	@Override
	public void build()
	{
		// General settings
		define("ignore_totem", false, "Killing the player ignores totems (enable must be true)");
	}
	
	@Override
	public void punish(ServerPlayer player)
	{
		// If we should ignore totems
		if(Boolean.valueOf(config.get("ignore_totem")))
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
	
	@Override
	Kill newInstance()
	{
		return new Kill();
	}
}