package io.github.freshsupasulley.censorcraft.config;

import io.github.freshsupasulley.LibraryLoader;
import net.minecraftforge.fml.config.ModConfig;

public class ClientConfig extends RawConfig {
	
	public static final int MIN_LATENCY = 100, MAX_LATENCY = 5000;
	
	public static ConfigValue<Boolean> SHOW_TRANSCRIPTION, DEBUG, USE_VULKAN;
	public static ConfigValue<Integer> LATENCY;
	
	public ClientConfig()
	{
		super(ModConfig.Type.CLIENT);
	}
	
	@Override
	protected void register()
	{
		SHOW_TRANSCRIPTION = newConfig("general.show_transcription", false).build();
		DEBUG = newConfig("general.debug", false).build();
		LATENCY = newConfig("general.latency", 1000).setRange(MIN_LATENCY, MAX_LATENCY).build();// .addValidator(t -> t > MIN_LATENCY && t < MAX_LATENCY));
		USE_VULKAN = newConfig("general.use_vulkan", LibraryLoader.canUseVulkan()).build();
	}
}
