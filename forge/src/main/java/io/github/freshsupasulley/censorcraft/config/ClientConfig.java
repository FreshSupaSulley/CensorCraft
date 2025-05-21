package io.github.freshsupasulley.censorcraft.config;

import io.github.freshsupasulley.LibraryLoader;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

public class ClientConfig extends Config {
	
	public static final int MIN_LATENCY = 100, MAX_LATENCY = 5000;
	
	public static ConfigValue<Boolean> SHOW_TRANSCRIPTION, SHOW_VOLUME_BAR, DEBUG, SHOW_VAD, VAD, DENOISE,
										USE_VULKAN;
	public static ConfigValue<Integer> LATENCY;
	public static ConfigValue<String> PREFERRED_MIC;
	
	@Override
	public ForgeConfigSpec register()
	{
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		builder.push("general");
		PREFERRED_MIC = builder.comment("Name of microphone, otherwise uses first available").define("preferred_mic", "");
		// INDICATE_RECORDING = builder.comment("Shows permanent text in-game indicating recording status").define("indicate_recording", true);
		SHOW_TRANSCRIPTION = builder.comment("Display live transcription").define("show_transcription", true);
		SHOW_VOLUME_BAR = builder.comment("Display microphone volume").define("show_mic_volume", false);
		SHOW_VAD = builder.comment("Shows when you are speaking. VAD must be true").define("show_vad", false);
		DEBUG = builder.comment("Shows helpful debugging information").define("debug", false);
		LATENCY = builder.comment("Transcription latency (in milliseconds). Internally represents the size of an individual audio sample").defineInRange("latency", 1000, MIN_LATENCY, MAX_LATENCY);
		USE_VULKAN = builder.comment("Uses Vulkan-built libraries for Windows GPU support. Can break on some machines").define("use_vulkan", LibraryLoader.canUseVulkan());
		
		// Recording
		builder.pop().comment("Only mess with these settings if you know what you're doing").push("experimental");
		VAD = builder.comment("Voice activity detection").define("vad", true);
		DENOISE = builder.comment("Denoise. Helps with VAD").define("denoise", true);
		
		return builder.build();
	}
}
