package io.github.freshsupasulley.censorcraft.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigSpec;

import io.github.freshsupasulley.LibraryLoader;
import net.minecraftforge.fml.config.ModConfig;

public class ClientConfig extends ConfigFile {
	
	public static final int MIN_LATENCY = 100, MAX_LATENCY = 5000;
	
	public ClientConfig()
	{
		super(ModConfig.Type.CLIENT);
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
		spec.define("show_transcription", false);
		spec.define("debug", false);
		spec.define("use_vulkan", LibraryLoader.canUseVulkan());
		spec.defineInRange("latency", 1000, MIN_LATENCY, MAX_LATENCY);
	}
	
	@Override
	void applyComments(CommentedConfig config)
	{
		config.setComment("show_transcription", "Display live transcriptions");
		config.setComment("debug", "Shows helpful debugging information");
		config.setComment("use_vulkan", "Uses Vulkan-built libraries for Windows GPU support. Can break on some machines");
		config.setComment("latency", "Transcription latency (in milliseconds). Internally represents the size of an individual audio sample");
	}
}
