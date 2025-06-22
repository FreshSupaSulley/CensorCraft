package io.github.freshsupasulley.censorcraft.config;

import java.nio.file.Path;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileNotFoundAction;
import com.electronwill.nightconfig.core.io.WritingMode;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;

public abstract class ConfigFile {
	
	private Path path;
	
	public ConfigFile(ModConfig.Type type)
	{
		CommentedFileConfig config = CommentedFileConfig.builder(FMLPaths.CONFIGDIR.get().resolve(CensorCraft.MODID + "-" + type.extension() + ".toml")).autosave().autoreload().writingMode(WritingMode.REPLACE).sync().preserveInsertionOrder().onFileNotFound(FileNotFoundAction.CREATE_EMPTY).build();
		
		try
		{
			config.load();
		} catch(Exception e)
		{
			CensorCraft.LOGGER.error("Config file is malformed at {}!", config.getNioPath());
			// throw e;
		}
		
		this.path = config.getNioPath();
		
		register(new ConfigWrapper(config));
		
		config.save();
	}
	
	abstract void register(ConfigWrapper config);
	
	public Path getFilePath()
	{
		return path;
	}
}
