package io.github.freshsupasulley.censorcraft.config;

import java.nio.file.Path;

import io.github.freshsupasulley.LibraryLoader;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.config.ModConfig.Type;

public class ClientConfig extends Config {
	
	public static final int MIN_LATENCY = 100, MAX_LATENCY = 5000;
	
	public static ConfigValue<Boolean> SHOW_TRANSCRIPTION, DEBUG, USE_VULKAN;
	public static ConfigValue<Integer> LATENCY;
	
	public static Path filePath;
	
	@Override
	public ForgeConfigSpec register()
	{
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		builder.push("general");
		SHOW_TRANSCRIPTION = builder.comment("Display live transcriptions").define("show_transcription", false);
//		INDICATE_TRANSCRIBING = builder.comment("Indicates when the audio feed will be transcribed").define("indicate_transcribing", true);
		DEBUG = builder.comment("Shows helpful debugging information").define("debug", false);
		LATENCY = builder.comment("Transcription latency (in milliseconds). Internally represents the size of an individual audio sample").defineInRange("latency", 1000, MIN_LATENCY, MAX_LATENCY);
		USE_VULKAN = builder.comment("Uses Vulkan-built libraries for Windows GPU support. Can break on some machines").define("use_vulkan", LibraryLoader.canUseVulkan());
		
		// Recording
		// builder.pop().comment("Only mess with these settings if you know what you're doing").push("experimental");
		return builder.build();
	}
	
	@Override
	protected Type getType()
	{
		return Type.CLIENT;
	}
	
	@Override
	protected void onConfigUpdate(ModConfig config)
	{
		ClientConfig.filePath = config.getFullPath();
	}
}
