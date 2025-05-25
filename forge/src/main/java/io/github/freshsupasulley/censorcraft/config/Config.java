package io.github.freshsupasulley.censorcraft.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public abstract class Config {
	
	private ForgeConfigSpec spec;
	
	public static void register(FMLJavaModLoadingContext context)
	{
		Config[] configs = {new ClientConfig(), new ServerConfig()};
		
		for(Config config : configs)
		{
			context.registerConfig(config.getType(), config.spec = config.register());
			context.getModEventBus().addListener(config::onConfigLoad);
			context.getModEventBus().addListener(config::onConfigReload);
		}
	}
	
	protected abstract ForgeConfigSpec register();
	protected abstract ModConfig.Type getType();
	protected abstract void onConfigUpdate(ModConfig config);
	
	private void checkConfig(ModConfig config)
	{
		// We're only checking the server config
		if(config.getType() != getType() || !spec.isLoaded())
			return;
		
		onConfigUpdate(config);
	}
	
	private void onConfigLoad(ModConfigEvent.Loading event)
	{
		checkConfig(event.getConfig());
	}
	
	private void onConfigReload(ModConfigEvent.Reloading event)
	{
		checkConfig(event.getConfig());
	}
}
