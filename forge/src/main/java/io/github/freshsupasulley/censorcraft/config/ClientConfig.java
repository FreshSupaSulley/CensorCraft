package io.github.freshsupasulley.censorcraft.config;

import java.util.function.Supplier;

import com.electronwill.nightconfig.core.serde.annotations.SerdeDefault;
import com.electronwill.nightconfig.core.serde.annotations.SerdeDefault.WhenValue;

import io.github.freshsupasulley.LibraryLoader;
import net.minecraftforge.fml.config.ModConfig;

public class ClientConfig extends ConfigFile {
	
	public static final int MIN_LATENCY = 100, MAX_LATENCY = 5000;
	
	@SerdeDefault(provider = "defaultShowTranscription", whenValue = WhenValue.IS_NULL)
	private Boolean showTranscription = defaultShowTranscription.get();
	static transient Supplier<Boolean> defaultShowTranscription = () -> false;
	
	@SerdeDefault(provider = "defaultDebug", whenValue = WhenValue.IS_NULL)
	private Boolean debug = defaultDebug.get();
	static transient Supplier<Boolean> defaultDebug = () -> false;
	
	@SerdeDefault(provider = "defaultUseVulkan", whenValue = WhenValue.IS_NULL)
	private Boolean useVulkan = defaultUseVulkan.get();
	static transient Supplier<Boolean> defaultUseVulkan = () -> LibraryLoader.canUseVulkan();
	
	@SerdeDefault(provider = "defaultLatency", whenValue = { WhenValue.IS_EMPTY, WhenValue.IS_MISSING, WhenValue.IS_NULL})
	public Long latency = defaultLatency.get();
	static transient Supplier<Long> defaultLatency = () -> 1000L;
	
	public ClientConfig()
	{
		super(ModConfig.Type.CLIENT);
	}
	
	public boolean isShowTranscription()
	{
		return showTranscription;
	}
	
	public void setShowTranscription(boolean showTranscription)
	{
		this.showTranscription = showTranscription;
	}
	
	public boolean isDebug()
	{
		return debug;
	}
	
	public void setDebug(boolean debug)
	{
		this.debug = debug;
	}
	
	public boolean isUseVulkan()
	{
		return useVulkan;
	}
	
	public void setUseVulkan(boolean useVulkan)
	{
		this.useVulkan = useVulkan;
	}
	
	public long getLatency()
	{
		return latency;
	}
	
	public void setLatency(long latency)
	{
		this.latency = latency;
	}
}
