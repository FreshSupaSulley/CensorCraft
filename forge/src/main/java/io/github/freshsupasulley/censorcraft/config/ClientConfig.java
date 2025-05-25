package io.github.freshsupasulley.censorcraft.config;

import java.nio.file.Path;

import io.github.freshsupasulley.LibraryLoader;
import io.github.givimad.libfvadjni.VoiceActivityDetector;
import io.github.givimad.libfvadjni.VoiceActivityDetector.Mode;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.config.ModConfig.Type;

public class ClientConfig extends Config {
	
	public static final int MIN_IS = 0, MAX_IS = 1;
	public static final int MIN_LATENCY = 100, MAX_LATENCY = 5000;
	
	public static ConfigValue<Boolean> SHOW_TRANSCRIPTION, SHOW_VOLUME_BAR, DEBUG, INDICATE_TRANSCRIBING, VAD, DENOISE,
										USE_VULKAN;
	
	public static ConfigValue<VoiceActivityDetector.Mode> VAD_MODE;
	
	public static ConfigValue<Float> INPUT_SENSITIVITY;
	public static ConfigValue<Integer> LATENCY;
	public static ConfigValue<String> PREFERRED_MIC;
	
	public static Path filePath;
	
	@Override
	public ForgeConfigSpec register()
	{
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		builder.push("general");
		PREFERRED_MIC = builder.comment("Name of microphone, otherwise uses first available").define("preferred_mic", "");
		// INDICATE_RECORDING = builder.comment("Shows permanent text in-game indicating recording status").define("indicate_recording", true);
		SHOW_TRANSCRIPTION = builder.comment("Display live transcription").define("show_transcription", true);
		SHOW_VOLUME_BAR = builder.comment("Display microphone volume").define("show_mic_volume", false);
		INDICATE_TRANSCRIBING = builder.comment("Indicates when the audio feed will be transcribed").define("indicate_transcribing", true);
		INPUT_SENSITIVITY = builder.comment("Minimum volume to transcribe").defineInRange("input_sensitivity", 0.2f, MIN_IS, MAX_IS);
		DEBUG = builder.comment("Shows helpful debugging information").define("debug", false);
		LATENCY = builder.comment("Transcription latency (in milliseconds). Internally represents the size of an individual audio sample").defineInRange("latency", 1000, MIN_LATENCY, MAX_LATENCY);
		USE_VULKAN = builder.comment("Uses Vulkan-built libraries for Windows GPU support. Can break on some machines").define("use_vulkan", LibraryLoader.canUseVulkan());
		
		// Recording
		builder.pop().comment("Only mess with these settings if you know what you're doing").push("experimental");
		VAD = builder.comment("Voice activity detection").define("vad", true);
		VAD_MODE = builder.comment("VAD setting. Determines the aggressiveness").defineEnum("vad_mode", Mode.VERY_AGGRESSIVE);
		DENOISE = builder.comment("Denoise. Helps with VAD").define("denoise", true);
		
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
