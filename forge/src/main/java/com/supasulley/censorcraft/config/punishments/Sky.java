package com.supasulley.censorcraft.config.punishments;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

public class Sky extends PunishmentOption {
	
	public static ConfigValue<Boolean> TELEPORT;
	public static ConfigValue<Float> LAUNCH_HEIGHT;
	
	@Override
	public void build(Builder builder)
	{
		LAUNCH_HEIGHT = builder.comment("Launch velocity").defineInRange("launch_velocity", 100f, Float.MIN_VALUE, Float.MAX_VALUE);
		TELEPORT = builder.comment("Teleports the player to the sky instead of launching them").comment("Adds launch_height to the player's Y position").define("teleport", false);
	}
	
	@Override
	public void punish(ServerPlayer player)
	{
		Vec3 pos = player.position();
		
		if(TELEPORT.get())
		{
			player.teleportTo(pos.x, pos.y + LAUNCH_HEIGHT.get(), pos.z);
		}
		else
		{
			player.addDeltaMovement(new Vec3(0, LAUNCH_HEIGHT.get(), 0));
			player.hurtMarked = true;
		}
	}
}
