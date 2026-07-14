package io.github.freshsupasulley.censorcraft.punishments;

import io.github.freshsupasulley.censorcraft.api.punishments.PunishmentJob;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

public class Kill extends BuiltInPunishment {
	
	@Override
	public String getId()
	{
		return "kill";
	}
	
	@Override
	public void buildConfig()
	{
		define("ignore_totem", false, "Killing the player ignores totems (enable must be true)");
	}
	
	@Override
	public PunishmentJob punish(ServerPlayer player, Set<String> taboos)
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
		
		return null;
	}
}