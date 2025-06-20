package io.github.freshsupasulley.censorcraft.config;

import io.github.freshsupasulley.LibraryLoader;

public class ClientConfig extends RawConfig {
	
	public static final int MIN_LATENCY = 100, MAX_LATENCY = 5000;
	
	public static ConfigValue<Boolean> SHOW_TRANSCRIPTION, DEBUG, USE_VULKAN;
	public static ConfigValue<Integer> LATENCY;
	
	public ClientConfig()
	{
		super("client");
	}
	
	@Override
	protected void register()
	{
		SHOW_TRANSCRIPTION = add(new ConfigValueBuilder<Boolean>("general.show_transcription", false));
		DEBUG = add(new ConfigValueBuilder<Boolean>("general.debug", false));
		LATENCY = add(new ConfigValueBuilder<Integer>("general.latency", 1000).addValidator(t -> t > MIN_LATENCY && t < MAX_LATENCY));
		USE_VULKAN = add(new ConfigValueBuilder<Boolean>("general.use_vulkan", LibraryLoader.canUseVulkan()));
	}
}
