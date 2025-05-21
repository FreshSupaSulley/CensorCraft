package io.github.freshsupasulley.censorcraft.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public abstract class Config {
	
	public static void register(FMLJavaModLoadingContext context)
	{
		context.registerConfig(ModConfig.Type.CLIENT, new ClientConfig().register());
		context.registerConfig(ModConfig.Type.SERVER, new ServerConfig().register());
	}
	
	protected abstract ForgeConfigSpec register();
}
