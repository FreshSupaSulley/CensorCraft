package io.github.freshsupasulley.censorcraft.config;

import java.nio.file.Path;
import java.util.Locale;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileNotFoundAction;
import com.electronwill.nightconfig.core.file.GenericBuilder;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.core.serde.ObjectSerializer;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLConfig;
import net.minecraftforge.fml.loading.FMLPaths;

public abstract class ConfigFile {
	
	protected transient CommentedFileConfig config;
	
	public ConfigFile(ModConfig.Type type)
	{
		System.out.println("SETTING UP" + FMLConfig.getConfigValue(FMLConfig.ConfigValue.DEFAULT_CONFIG_PATH));
		Path file = FMLPaths.GAMEDIR.get().resolve(String.format(Locale.ROOT, "%s-%s.toml", CensorCraft.MODID, type.extension()));
		System.out.println("gOT IT AT " + file.toAbsolutePath());
		
		// Setting autosave means it will not allow program to die... same with autoreload (2 things I need)
		GenericBuilder<CommentedConfig, CommentedFileConfig> builder = CommentedFileConfig.builder(file)/* .autosave() */
											.writingMode(WritingMode.REPLACE).sync().preserveInsertionOrder().onFileNotFound(FileNotFoundAction.CREATE_EMPTY);
		
		try(CommentedFileConfig config = builder.build())
		{
			config.load();
			this.config = config;
			
			config.addAll(ObjectSerializer.standard().serializeFields(this, () -> config.createSubConfig()));
			config.save();
			System.out.println("done w " + file);
		} catch(Exception e)
		{
			System.err.println("Config file is malformed at {}!" + getFilePath());
			// throw e;
		}
	}
	
	// abstract void register(ConfigWrapper config);
	
	public Path getFilePath()
	{
		return config.getNioPath();
	}
}
