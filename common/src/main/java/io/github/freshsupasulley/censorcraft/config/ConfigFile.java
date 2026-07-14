package io.github.freshsupasulley.censorcraft.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.ConfigSpec.CorrectionListener;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.ConfigLoadFilter;
import io.github.freshsupasulley.censorcraft.CensorCraft;
import io.github.freshsupasulley.censorcraft.api.punishments.IConfigFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A custom config file handler.
 */
public abstract class ConfigFile implements IConfigFile {
	
	static
	{
		// For ConfigSpecs. preserveInsertionOrder() on the main CommentedFileConfig isn't enough
		Config.setInsertionOrderPreserved(true);
	}
	
	private static final CorrectionListener LISTENER = (action, path, incorrectValue, correctedValue) ->
	{
		String pathString = String.join(".", path);
		CensorCraft.LOGGER.warn("Corrected '{}': was '{}', is now '{}'", pathString, incorrectValue, correctedValue);
	};
	
	public final CommentedFileConfig config;
	
	private final ConfigSpec spec = new ConfigSpec();
	private final Map<String, List<String>> comments = new HashMap<>();
	
	public ConfigFile(Path configFolder, String extension)
	{
		Path configFile = configFolder.resolve(String.format(Locale.ROOT, "%s-%s.toml", CensorCraft.MODID, extension));
		boolean newFile = Files.notExists(configFile);
		
		// This creates the file due to the default onFileNotFound
		config = CommentedFileConfig.builder(configFile).autosave().autoreload().sync().preserveInsertionOrder().onLoadFilter(new ConfigLoadFilter() {
			
			@Override
			public boolean acceptNewVersion(CommentedConfig newConfig)
			{
				if(!spec.isCorrect(newConfig))
				{
					CensorCraft.LOGGER.warn("{} is not correct", configFile);
					// Forge has a convenient way to store backups
					//					ConfigFileTypeHandler.backUpConfig(config);
					createBackup();
					spec.correct(newConfig, LISTENER);
					config.save();
				}
				
				return true;
			}
		}).build();
		
		register(spec);
		
		// Without this check, it will load a blank file and thus correct itself, needlessly creating a blank backup file
		if(!newFile)
		{
			// In case the file is bad on load, log and correct it
			try
			{
				config.load();
			} catch(Exception e)
			{
				CensorCraft.LOGGER.error("Failed loading config file: {}", configFile.toAbsolutePath(), e);
				createBackup();
			}
		}
		
		// Apply comments
		comments.forEach((key, value) -> config.setComment(key, value.stream().collect(Collectors.joining(System.lineSeparator()))));
		
		spec.correct(config, LISTENER);
		config.save();
	}
	
	/**
	 * Create a copy of the current config as a backup so the user can refer to it later.
	 */
	protected abstract void createBackup();
	
	abstract void register(ConfigSpec spec);
	
	@Override
	public ConfigSpec getSpec()
	{
		return spec;
	}
	
	@Override
	public Map<String, List<String>> getComments()
	{
		return comments;
	}
	
	public Path getFilePath()
	{
		return config.getNioPath();
	}
}
