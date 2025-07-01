package io.github.freshsupasulley.censorcraft.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	
	private Map<String, List<String>> comments = new HashMap<String, List<String>>();
	
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
		
		// Apply comments
		comments.forEach((key, value) ->
		{
			config.setComment(key, value.stream().collect(Collectors.joining(System.getProperty("line.separator"))));
		});
		
		spec.correct(config, LISTENER);
		config.save();
	}
	
	/**
	 * Convenience method to define a value within a range.
	 * 
	 * @param <T>          {@link Comparable} type
	 * @param key          config key
	 * @param description  config comment
	 * @param defaultValue initial value (must be within the range)
	 * @param min          minimum value
	 * @param max          maximum value
	 */
	<T extends Comparable<? super T>> void defineInRange(String key, String comment, T defaultValue, T min, T max)
	{
		Predicate<Object> validator = v ->
		{
			if(v == null)
				return false;
			
			@SuppressWarnings("unchecked")
			T val = (T) v;
			return min.compareTo(val) <= 0 && max.compareTo(val) >= 0;
		};
		
		if(validator.test(defaultValue))
			throw new IllegalStateException("Default value fails range validation");
		
		addComment(key, comment, "Range: " + min + " ~ " + max);
		spec.define(key, defaultValue, validator);
	}
	
	/**
	 * Convenience method to add a multi-line comment.
	 * 
	 * @param key      key
	 * @param comments list of comments
	 */
	void addComment(String key, String... comments)
	{
		this.comments.computeIfAbsent(key, list -> new ArrayList<String>(comments.length)).addAll(Stream.of(comments).toList());
	}
	
	abstract void register(ConfigSpec spec);
	
	/**
	 * Use this method to define comments with {@link ConfigFile#addComment(String, String...)}}.
	 */
	abstract void postLoad();
	
	public Path getFilePath()
	{
		return config.getNioPath();
	}
}
