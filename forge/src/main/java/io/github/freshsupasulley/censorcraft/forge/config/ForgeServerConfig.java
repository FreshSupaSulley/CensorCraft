package io.github.freshsupasulley.censorcraft.forge.config;

import io.github.freshsupasulley.censorcraft.config.ServerConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.config.ConfigFileTypeHandler;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ForgeServerConfig extends ServerConfig {
	
	private static final LevelResource SERVERCONFIG = new LevelResource("serverconfig");
	
	public ForgeServerConfig(MinecraftServer server)
	{
		super(getServerConfigPath(server));
	}
	
	/**
	 * Ripped from {@link ServerLifecycleHooks}.
	 *
	 * @param server MC server
	 * @return path to server config file
	 */
	private static Path getServerConfigPath(final MinecraftServer server)
	{
		final Path serverConfig = server.getWorldPath(SERVERCONFIG);
		
		if(!Files.isDirectory(serverConfig))
		{
			try
			{
				Files.createDirectories(serverConfig);
			} catch(IOException e)
			{
				throw new RuntimeException(e);
			}
		}
		
		return serverConfig;
	}
	
	@Override
	protected void createBackup()
	{
		// Forge has a convenient way to store backups
		ConfigFileTypeHandler.backUpConfig(config);
	}
}
