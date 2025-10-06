package io.github.freshsupasulley.censorcraft.punishments;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import net.minecraft.server.level.ServerPlayer;

public class Crash extends BuiltInPunishment {
	
	private int seconds;
	
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
	
	// Crashing will do nothing server-side (maybe we can kick them first or something)
	@Override
	public void punish(ServerPlayer player)
	{
		this.seconds = config.getInt("seconds");
	}
	
	@Override
	public void punishClientSide()
	{
		try
		{
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