package io.github.freshsupasulley.censorcraft.config.punishments;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import io.github.freshsupasulley.censorcraft.api.punishments.ClientPunishment;

public class Crash extends ClientPunishment {
	
	@Override
	public String getId()
	{
		return "crash";
	}
	
	@Override
	public void buildConfig()
	{
		defineInRange("seconds", 0, 0, Integer.MAX_VALUE, "Delay (in seconds) before Minecraft crashes (creates a \"Not Responding\" screen)");
	}
	
	@Override
	public void punish()
	{
		try
		{
			int seconds = config.getInt("seconds");
			CensorCraft.LOGGER.info("Waiting {} seconds before crashing", seconds);
			Thread.sleep((long) (seconds * 1000L));
			
			// Now close
			CensorCraft.LOGGER.info("Exiting Minecraft (get trolled)");
			System.exit(0);
		} catch(InterruptedException e)
		{
			CensorCraft.LOGGER.error("Failed executing crash punishment", e);
		}
	}
}