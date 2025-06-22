package io.github.freshsupasulley.censorcraft.config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.electronwill.nightconfig.core.AbstractConfig;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigFormat;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.ConfigSpec.CorrectionListener;
import com.electronwill.nightconfig.core.InMemoryFormat;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.ParsingException;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import net.minecraftforge.fml.config.ConfigFileTypeHandler;

public class ConfigWrapper extends AbstractConfig {
	
	private ConfigSpec spec;
	private CommentedFileConfig file;
	private CommentedConfig config;
	
	private String prefix;
	
	private ConfigWrapper(CommentedFileConfig file, CommentedConfig config, ConfigSpec spec, String prefix)
	{
		super(true);
		
		this.file = file;
		this.config = config;
		this.spec = spec;
		this.prefix = prefix;
	}
	
	public ConfigWrapper(CommentedFileConfig config)
	{
		this(config, config, new ConfigSpec(), "");
	}
	
	// public ConfigWrapper(CommentedConfig config, ConfigSpec spec)
	// {
	// this("", spec);
	// }
	
	<T> ConfigValueBuilder<T> builder(String name, T value)
	{
		return new ConfigValueBuilder<T>(prefix + name, value);
	}
	
	ConfigWrapper sub(String name)
	{
		String subPrefix = prefix + name;
		// CommentedConfig sub = config.createSubConfig();
		// config.set(subPrefix, sub);
		// System.out.println("SUBBING AT " + name + " " + subPrefix + " " + prefix);
		return new ConfigWrapper(file, file, spec, subPrefix + ".");
	}
	
//	public ConfigValue<List<CommentedConfig>> buildTable(String key, Consumer<ConfigWrapper> configSetup)
//	{
//		ConfigWrapper tableEntry = createSubConfig();
//		configSetup.accept(tableEntry);
//		return builder(key, List.of(tableEntry.config)).build();
//	}
	
	public ConfigValue<List<ConfigWrapper>> buildTable(String key, Consumer<ConfigWrapper> configSetup)
	{
		ConfigWrapper tableEntry = createSubConfig();
		configSetup.accept(tableEntry);
		return builder(key, List.of(tableEntry)).build();
	}
	
	class ConfigValueBuilder<T> {
		
		private final String key;
		private final T defaultValue;
		
		private List<String> comments = new ArrayList<String>();
		private Predicate<T> validator = (t) -> true;
		
		private ConfigValueBuilder(String key, T defaultValue)
		{
			this.key = key;
			this.defaultValue = defaultValue;
		}
		
		public ConfigValueBuilder<T> setValidator(Predicate<T> validator)
		{
			if(!validator.test(defaultValue))
			{
				throw new IllegalArgumentException("Default value fails validation");
			}
			
			this.validator = validator;
			return this;
		}
		
		@SuppressWarnings("unchecked")
		public ConfigValueBuilder<T> setRange(T min, T max)
		{
			if(!(min instanceof Comparable) || !(max instanceof Comparable))
			{
				throw new IllegalArgumentException("Type T must be Comparable to use setRange");
			}
			
			Comparable<T> minC = (Comparable<T>) min;
			Comparable<T> maxC = (Comparable<T>) max;
			
			Predicate<T> rangeValidator = (T value) -> minC.compareTo(value) <= 0 && maxC.compareTo(value) >= 0;
			return setValidator(rangeValidator);
		}
		
		public ConfigValueBuilder<T> addComment(String comment)
		{
			// I like a little spacing
			this.comments.add(" " + comment);
			return this;
		}
		
		@SuppressWarnings("unchecked")
		public ConfigValue<T> build()
		{
			System.out.println("BUILDING AT " + key);
			if(!config.contains(key))
			{
				config.set(key, defaultValue);
			}
			
			if(!comments.isEmpty())
			{
				config.setComment(key, comments.stream().collect(Collectors.joining(System.getProperty("line.separator"))));
			}
			
			spec.define(key, defaultValue, (value) -> defaultValue.getClass().isInstance(value) && validator.test((T) value));
			return new ConfigValue<T>(key, defaultValue, validator);
		}
	}
	
	public class ConfigValue<T> {
		
		private final String key;
		private final T defaultValue;
		private Predicate<T> validator;
		
		public ConfigValue(String key, T defaultValue, Predicate<T> validator)
		{
			this.key = key;
			this.defaultValue = defaultValue;
			this.validator = validator;
		}
		
		public T get()
		{
			try
			{
				Object rawValue = config.get(key);
				
				if(spec.isCorrect(key, rawValue))// && defaultValue.getClass().isInstance(rawValue)) // i wonder if we need the defaultValue isInstance check anymore
				{
					@SuppressWarnings("unchecked")
					T value = (T) rawValue;
					
					if(validator.test(value))
					{
						// Put the file back if the user deletes it
						if(!file.getFile().exists())
						{
							CensorCraft.LOGGER.warn("File is missing! Recreating...");
							file.save();
						}
						
						return value;
					}
				}
				
				// Pass to corrector
				throw new ParsingException("Invalid config value for key " + key + ": " + rawValue);
			} catch(ClassCastException | ParsingException e)
			{
				CensorCraft.LOGGER.warn("Configuration file {} is not correct. Correcting", file.getNioPath(), key);
				CensorCraft.LOGGER.debug("Config file exception", e);
				
				// Store the user's changes in a separate file so they can repair it easier
				CensorCraft.LOGGER.warn("Created backup config file");
				ConfigFileTypeHandler.backUpConfig(file);
				
				CorrectionListener listener = (action, path, incorrectValue, correctedValue) ->
				{
					String pathString = String.join(".", path);
					CensorCraft.LOGGER.debug("Corrected '{}': was '{}', is now '{}'", pathString, incorrectValue, correctedValue);
				};
				
				spec.correct(config, listener);
				file.save();
				
				// set(defaultValue);
				return defaultValue;
			}
		}
		
		public void set(T value)
		{
			if(validator.test(value))
			{
				config.set(key, value);
			}
		}
		
		@Override
		public String toString()
		{
			return super.toString() + " - " + key;
		}
	}
	
	@Override
	public ConfigWrapper createSubConfig()
	{
		// throw new IllegalStateException("Method not implemented");
		CommentedConfig sub = CommentedConfig.inMemory(); // create isolated inner config
		return new ConfigWrapper(file, sub, spec, ""); // note: prefix left blank
		// return new ConfigWrapper(newSub, spec, "");
	}
	
	@Override
	public ConfigFormat<?> configFormat()
	{
		return InMemoryFormat.defaultInstance();
	}
	
	@Override
	public AbstractConfig clone()
	{
		throw new IllegalStateException("Method not implemented");
		// return null;
	}
}
