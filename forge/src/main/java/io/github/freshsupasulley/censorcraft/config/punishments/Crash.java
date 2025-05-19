package io.github.freshsupasulley.censorcraft.config.punishments;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

public class Crash extends PunishmentOption {
	
	private static ConfigValue<Float> DELAY_SECONDS;
	
	@Override
	public String getDescription()
	{
		return "Crashes Minecraft for the player that spoke the word";
	}
	
	@Override
	public void build(Builder builder)
	{
		builder.comment("Using RANDOM means the player will be sent to a dimension they are not already in");
		DELAY_SECONDS = builder.comment("Delay (in seconds) before Minecraft crashes (creates a \"Not Responding\" screen)").defineInRange("seconds", 0, 3f, Float.MAX_VALUE);
	}
	
	@Override
	public void punish(ServerPlayer player)
	{
		// Presence of player means this is server-side. This is a client-side executed punishment only
		if(player != null) return;
		
		try
		{
			Thread.sleep((long) (DELAY_SECONDS.get() * 1000L));
		} catch(InterruptedException e)
		{
			e.printStackTrace();
		} finally
		{
			CensorCraft.LOGGER.info("Exiting Minecraft (get trolled)");
			System.exit(0);
		}
	}
}