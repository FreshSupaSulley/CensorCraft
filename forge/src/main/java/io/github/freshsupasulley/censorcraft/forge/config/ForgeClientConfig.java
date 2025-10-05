package io.github.freshsupasulley.censorcraft.forge.config;

import io.github.freshsupasulley.censorcraft.common.config.ClientConfig;
import net.minecraftforge.fml.config.ConfigFileTypeHandler;
import net.minecraftforge.fml.loading.FMLPaths;

public class ForgeClientConfig extends ClientConfig {
	
	public ForgeClientConfig()
	{
		super(FMLPaths.CONFIGDIR.get());
	}
	
	@Override
	protected void createBackup()
	{
		ConfigFileTypeHandler.backUpConfig(config);
	}
}
