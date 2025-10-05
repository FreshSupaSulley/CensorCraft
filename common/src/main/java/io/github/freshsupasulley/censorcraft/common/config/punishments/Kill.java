package io.github.freshsupasulley.censorcraft.common.config.punishments;

import net.minecraft.server.level.ServerPlayer;

public class Kill extends BuiltInPunishment {
	
	@Override
	public String getId()
	{
		return "kill";
	}
	
	@Override
	public void buildConfig()
	{
		// General settings
		define("ignore_totem", false, "Killing the player ignores totems (enable must be true)");
	}
	
	@Override
	public void punish(ServerPlayer player)
	{
		boolean ignoreTotem = config.get("ignore_totem");
		
		// If we should ignore totems
		if(ignoreTotem)
		{
			// Generic kill ignores totems
			player.kill(player.level());
		}
		else
		{
			// Generic will stop at totems
			player.hurtServer(player.level(), player.level().damageSources().generic(), Float.MAX_VALUE);
		}
	}
}