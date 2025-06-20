package io.github.freshsupasulley.censorcraft.config;

import io.github.freshsupasulley.LibraryLoader;

public class ClientConfig extends RawConfig {
	
	public static final int MIN_LATENCY = 100, MAX_LATENCY = 5000;
	
	public ClientConfig()
	{
		super("client");
	}
	
	@Override
	protected void register()
	{
		setDefault(config, "general.show_transcription", showTranscription(), "Display live transcriptions");
		setDefault(config, "general.debug", debug(), "Shows helpful debugging information");
		setDefault(config, "general.latency", getLatency(), "Transcription latency in milliseconds");
		setDefault(config, "general.use_vulkan", useVulkan(), "Use Vulkan-based GPU libraries (Windows only)");
	}
	
	public static boolean showTranscription()
	{
		return config.getOrElse("general.show_transcription", false);
	}
	
	public static boolean debug()
	{
		return config.getOrElse("general.debug", false);
	}
	
	public static boolean useVulkan()
	{
		return config.getOrElse("general.use_vulkan", LibraryLoader.canUseVulkan());
	}
	
	public static int getLatency()
	{
		return config.getOrElse("general.latency", 1000);
	}
}
