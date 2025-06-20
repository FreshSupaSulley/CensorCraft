package io.github.freshsupasulley.censorcraft.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileNotFoundAction;
import com.electronwill.nightconfig.core.io.WritingMode;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import net.minecraftforge.fml.loading.FMLPaths;

public abstract class RawConfig {
	
	protected static CommentedFileConfig config;
	
	public RawConfig(String name)
	{
		config = CommentedFileConfig.builder(FMLPaths.CONFIGDIR.get().resolve(CensorCraft.MODID + name + ".toml")).autosave().autoreload().writingMode(WritingMode.REPLACE).sync().preserveInsertionOrder().onFileNotFound(FileNotFoundAction.CREATE_EMPTY).build();
		config.load();
		
		register();
		
		config.save();
	}
	
	protected abstract void register();
	
	<T> void setDefault(CommentedFileConfig config, String key, T defaultValue, String comment)
	{
		if(!config.contains(key))
		{
			config.set(key, defaultValue);
		}
		
		config.setComment(key, comment);
	}
}
