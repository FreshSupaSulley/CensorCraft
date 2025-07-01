package io.github.freshsupasulley.censorcraft.config;

import java.nio.file.Path;
import java.util.Locale;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.ConfigSpec.CorrectionListener;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.ConfigLoadFilter;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import net.minecraftforge.fml.config.ConfigFileTypeHandler;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;

// System.out.println("SETTING UP" + FMLConfig.getConfigValue(FMLConfig.ConfigValue.DEFAULT_CONFIG_PATH));
/**
 * A custom config file handler, because Forge is lacking support for array of tables.
 * 
 * <h1>KNOWN QUIRKS:</h1>
 * <p>
 * Pretty much exclusively use int and double, as that's what NightConfig likes. If not, the config will come back as incorrect every reload.
 * </p>
 */
public abstract class ConfigFile {
	
	protected final CommentedFileConfig config;
	protected final ConfigSpec spec = new ConfigSpec();
	
	private static final CorrectionListener LISTENER = (action, path, incorrectValue, correctedValue) ->
	{
		String pathString = String.join(".", path);
		CensorCraft.LOGGER.warn("Corrected '{}': was '{}', is now '{}'", pathString, incorrectValue, correctedValue);
	};
	
	public ConfigFile(ModConfig.Type type)
	{
		Path file = FMLPaths.GAMEDIR.get().resolve(String.format(Locale.ROOT, "%s-%s.toml", CensorCraft.MODID, type.extension()));
		config = CommentedFileConfig.builder(file).autosave().autoreload().sync().onLoadFilter(new ConfigLoadFilter()
		{
			@Override
			public boolean acceptNewVersion(CommentedConfig newConfig)
			{
				if(!spec.isCorrect(newConfig))
				{
					CensorCraft.LOGGER.warn("{} is not correct", file);
					// Forge has a convenient way to store backups
					ConfigFileTypeHandler.backUpConfig(config);
					spec.correct(newConfig, LISTENER);
					config.save();
				}
				
				return true;
			}
		}).build();
		
		register(spec);
		
		config.load();
		
		// Ig you have to do this AFTER the load
		applyComments(config);
		spec.correct(config, LISTENER);
		config.save();
	}
	
	abstract void register(ConfigSpec spec);
	
	abstract void applyComments(CommentedConfig config);
	
	public Path getFilePath()
	{
		return config.getNioPath();
	}
}
