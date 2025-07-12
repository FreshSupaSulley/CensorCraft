package io.github.freshsupasulley.censorcraft.config;

import com.electronwill.nightconfig.core.ConfigSpec;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import io.github.freshsupasulley.censorcraft.ClientCensorCraft;
import io.github.freshsupasulley.censorcraft.gui.ConfigScreen;
import io.github.freshsupasulley.whisperjni.LibraryUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;

// dist has to be client here, otherwise dedicated servers will try to load the ConfigScreen class and shit the bed
@Mod.EventBusSubscriber(modid = CensorCraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientConfig extends ConfigFile {
	
	public static final int MIN_LATENCY = 100, MAX_LATENCY = 5000;
	
	private static ClientConfig CLIENT;
	
	@SubscribeEvent
	private static void clientSetup(FMLClientSetupEvent event)
	{
		MinecraftForge.registerConfigScreen((minecraft, screen) -> new ConfigScreen(minecraft, screen));
		CLIENT = new ClientConfig();
	}
	
	public static ClientConfig get()
	{
		if(CLIENT == null)
		{
			CensorCraft.LOGGER.error("Tried to access client config before it was initialized");
		}
		
		return CLIENT;
	}
	
	public ClientConfig()
	{
		super(FMLPaths.CONFIGDIR.get(), ModConfig.Type.CLIENT);
	}
	
	public boolean isShowTranscription()
	{
		return config.get("show_transcription");
	}
	
	public void setShowTranscription(boolean val)
	{
		config.set("show_transcription", val);
	}
	
	public boolean isDebug()
	{
		return config.get("debug");
	}
	
	public void setDebug(boolean val)
	{
		config.set("debug", val);
	}
	
	public boolean isUseVulkan()
	{
		return config.get("use_vulkan");
	}
	
	public void setUseVulkan(boolean val)
	{
		config.set("use_vulkan", val);
	}
	
	public int getLatency()
	{
		return config.getInt("latency");
	}
	
	public void setLatency(long val)
	{
		config.set("latency", val);
	}
	
	public int getGUIX()
	{
		return config.get("gui_x");
	}
	
	public void setGUIX(int val)
	{
		config.set("gui_x", val);
	}
	
	// Whisper JNI VAD settings
	public boolean getVAD()
	{
		return config.get("vad.enable");
	}
	
	public float getVADThreshold()
	{
		return ((Number) config.get("vad.threshold")).floatValue();
	}
	
	public int getVADMinSpeechDurationMS()
	{
		return config.getInt("vad.min_speech_duration_ms");
	}
	
	public int getVADMinSilenceDurationMS()
	{
		return config.getInt("vad.min_silence_duration_ms");
	}
	
	public float getVADMaxSpeechDurationS()
	{
		return ((Number) config.getByte("vad.max_speech_duration_s")).floatValue();
	}
	
	public int getVADSpeechPadMS()
	{
		return config.getInt("vad.speech_pad_ms");
	}
	
	public float getVADSamplesOverlap()
	{
		return ((Number) config.get("vad.samples_overlap")).floatValue();
	}
	
	@Override
	void register(ConfigSpec spec)
	{
		define("show_transcription", true, "Display live transcriptions");
		define("debug", false, "Shows helpful debugging information");
		define("use_vulkan", LibraryUtils.canUseVulkan(CensorCraft.LOGGER), "Uses Vulkan-built libraries for Windows GPU support. Can break on some machines");
		defineInRange("latency", 1000, MIN_LATENCY, MAX_LATENCY, "Transcription latency (in milliseconds). Internally represents the size of an individual audio sample");
		
		// GUI positioning
		define("gui_x", -ClientCensorCraft.PADDING, "GUI X position", "Negative values mean anchoring to the right instead");
		
		// VAD
		define("vad.enable", true, "Use voice activation detection filtering before processing transcription", "Changing these settings requires clicking the restart button in the mod config menu (or restarting the game)");
		defineInRange("vad.threshold", 0.5D, 0D, 1D, "Probability threshold to consider as speech");
		defineInRange("vad.min_speech_duration_ms", 200, 100, 1000, "Min duration for a valid speech segment");
		defineInRange("vad.min_silence_duration_ms", 100, 100, 1000, "Min silence duration to consider speech as ended");
		defineInRange("vad.max_speech_duration_s", 10D, 10D, 30D, "Max duration of a speech segment before forcing a new segment");
		defineInRange("vad.speech_pad_ms", 200, 0, 500, "Padding added before and after speech segments");
		defineInRange("vad.samples_overlap", 0.1D, 0D, 1D, "Overlap in seconds when copying audio samples from speech segment");
	}
}
