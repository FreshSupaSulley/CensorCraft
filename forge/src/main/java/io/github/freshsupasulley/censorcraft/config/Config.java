package io.github.freshsupasulley.censorcraft.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileNotFoundAction;
import com.electronwill.nightconfig.core.io.WritingMode;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

public abstract class Config {
	
	private ForgeConfigSpec spec;
	
	public static void register(FMLJavaModLoadingContext context)
	{
		Config[] configs = {new ServerConfig()};
		
		for(Config config : configs)
		{
			context.registerConfig(config.getType(), config.spec = config.register());
			context.getModEventBus().addListener(config::onConfigLoad);
			context.getModEventBus().addListener(config::onConfigReload);
		}
		
		new ClientConfig();
		
//		context.registerConfig(ModConfig.Type.CLIENT, config);
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
