package io.github.freshsupasulley.censorcraft.config;

import com.electronwill.nightconfig.core.ConfigSpec;

import io.github.freshsupasulley.LibraryLoader;
import io.github.freshsupasulley.censorcraft.CensorCraft;
import io.github.freshsupasulley.censorcraft.gui.ConfigScreen;
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
	
	@Override
	void register(ConfigSpec spec)
	{
		define("show_transcription", false, "Display live transcriptions");
		define("debug", false, "Shows helpful debugging information");
		define("use_vulkan", LibraryLoader.canUseVulkan(), "Uses Vulkan-built libraries for Windows GPU support. Can break on some machines");
		defineInRange("latency", 1000, MIN_LATENCY, MAX_LATENCY, "Transcription latency (in milliseconds). Internally represents the size of an individual audio sample");
	}
}
