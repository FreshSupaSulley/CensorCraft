package io.github.freshsupasulley.censorcraft.config.punishments;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import net.minecraft.server.level.ServerPlayer;

public class Crash extends ForgePunishment {
	
	@Override
	public void build()
	{
		defineInRange("seconds", 0, 0, Integer.MAX_VALUE, "Delay (in seconds) before Minecraft crashes (creates a \"Not Responding\" screen)");
	}
	
	@Override
	public void punish(ServerPlayer player)
	{
		// Presence of player means this is server-side. This is a client-side executed punishment only
		if(player != null)
			return;
		
		try
		{
			Thread.sleep((long) (config.getInt("seconds") * 1000L));
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