package io.github.freshsupasulley.censorcraft.config.punishments;

import net.minecraft.server.level.ServerPlayer;

public class Ignite extends PunishmentOption<Ignite> {
	
	@Override
	public String getDescription()
	{
		return "Sets the player on fire";
	}
	
	@Override
	public void build()
	{
		defineInRange("ignite_seconds", 5D, 0D, Double.MAX_VALUE, "Amount of seconds player is on fire for");
	}
	
	@Override
	public void punish(ServerPlayer player)
	{
		Number ignite = config.get("ignite_seconds");
		player.igniteForSeconds(ignite.floatValue());
	}
	
	@Override
	Ignite newInstance()
	{
		return new Ignite();
	}
}
