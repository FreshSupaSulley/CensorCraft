package io.github.freshsupasulley.censorcraft.common.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.ConfigSpec.CorrectionListener;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.ConfigLoadFilter;
import io.github.freshsupasulley.censorcraft.common.CensorCraft;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A custom config file handler, because Forge is lacking support for array of tables.
 *
 * <p>KNOWN QUIRKS:</p>
 *
 * <p>
 * Pretty much exclusively use int and double, as that's what NightConfig likes. If not, the config will come back as
 * incorrect every reload.
 * </p>
 */
public abstract class ConfigFile {
	
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
	
	protected final ConfigSpec spec = new ConfigSpec();
	
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
	
	/**
	 * Convenience method to bundle defining a value and a comment.
	 *
	 * @param <E>      value type
	 * @param key      config key
	 * @param value    initial value
	 * @param comments config comments
	 */
	final <E> void define(String key, E value, String... comments)
	{
		spec.define(key, value);
		addComment(key, comments);
	}
	
	/**
	 * Convenience method to bundle defining a list and a comment.
	 *
	 * @param key      config key
	 * @param value    initial value
	 * @param comments config comments
	 * @param <E>      value type
	 */
	final <E> void defineList(String key, List<E> value, String... comments)
	{
		spec.defineList(key, value, predicate -> true);
		addComment(key, comments);
	}
	
	/**
	 * Convenience method to define a value within a range.
	 *
	 * @param <T>          {@link Comparable} type
	 * @param key          config key
	 * @param defaultValue initial value (must be within the range)
	 * @param min          minimum value
	 * @param max          maximum value
	 * @param comments     config comments
	 */
	<T extends Comparable<? super T>> void defineInRange(String key, T defaultValue, T min, T max, String... comments)
	{
		Predicate<Object> validator = v ->
		{
			if(v == null)
				return false;
			
			@SuppressWarnings("unchecked")
			T val = (T) v;
			return min.compareTo(val) <= 0 && max.compareTo(val) >= 0;
		};
		
		if(!validator.test(defaultValue))
			throw new IllegalStateException("Default value fails range validation");
		
		// Range needs to go at the bottom
		Stream.concat(Stream.of(comments), Stream.of("Range: " + min + " ~ " + max)).forEach(comment -> addComment(key, comment));
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
		// I like a space between the pound and the comment
		this.comments.computeIfAbsent(key, list -> new ArrayList<>(comments.length)).addAll(Stream.of(comments).map(comment -> " " + comment).toList());
	}
	
	abstract void register(ConfigSpec spec);
	
	public Path getFilePath()
	{
		return config.getNioPath();
	}
}
