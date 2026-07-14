package io.github.freshsupasulley.censorcraft.punishments;

import io.github.freshsupasulley.censorcraft.api.punishments.PunishmentJob;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

public class Ignite extends BuiltInPunishment {
	
	@Override
	public String getId()
	{
		return "ignite";
	}
	
	@Override
	public void buildConfig()
	{
		defineInRange("ignite_seconds", 5D, 0D, Double.MAX_VALUE, "Amount of seconds player is on fire for");
	}
	
	@Override
	public PunishmentJob punish(ServerPlayer player, Set<String> taboos)
	{
		Number ignite = config.get("ignite_seconds");
		player.igniteForSeconds(ignite.floatValue());
		return null;
	}
}
