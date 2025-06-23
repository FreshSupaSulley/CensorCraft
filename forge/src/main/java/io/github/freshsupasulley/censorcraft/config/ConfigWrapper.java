package io.github.freshsupasulley.censorcraft.config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.ConfigSpec.CorrectionListener;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.ParsingException;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import net.minecraftforge.fml.config.ConfigFileTypeHandler;

public class ConfigWrapper {
	
	// Solely for IO operations
	CommentedFileConfig file;
	
	ConfigSpec spec;
	CommentedConfig config;
	
	protected ConfigWrapper(CommentedFileConfig file, CommentedConfig config)
	{
		this.file = file;
		this.spec = new ConfigSpec();
		this.config = config;
	}
	
	public ConfigWrapper(CommentedFileConfig file)
	{
		this(file, file);
	}
	
	public <T> ConfigValueBuilder<T> define(String name, T value)
	{
		return new ConfigValueBuilder<T>(name, value);
	}
	
	/**
	 * You can't use ConfigValues within an array of tables.
	 * 
	 * @param key
	 * @param configSetup
	 * @return
	 */
	public ConfigValue<List<CommentedConfig>> buildTable(String key, BiConsumer<CommentedConfig, ConfigSpec> configSetup, String... comments)
	{
		// ConfigWrapper table = config.createSubConfig();
		// UNWANTED = table.builder("unwanted_key", "abcdefg").addComment("LOVE YOU!!").build();
		// ConfigValue<List<ConfigWrapper>> tablerr = top.builder("tableee", List.of(table)).build();
		
		CommentedConfig tableEntry = config.createSubConfig();
		this.spec.define(key, tableEntry);
		
//		ConfigWrapper tableEntry = new ConfigWrapper(file, file.createSubConfig());
//		configSetup.accept(tableEntry.config, tableEntry.spec);
		
		ConfigSpec spec = new ConfigSpec();
		
		// Ensure all tables get a validation check
		ConfigValue<List<CommentedConfig>> result = define(key, List.of(tableEntry)).comment(comments).setValidator(table -> table.stream().allMatch(val -> spec.isCorrect(val))).build();
		
		configSetup.accept(tableEntry, spec);
		
		// Auto fills the config file
		System.err.println(spec.correct(tableEntry) + " corrections");
		
		return result;
	}
	
	public ConfigWrapper sub(String name, String... comments)
	{
		// String subPrefix = name;
		CommentedConfig sub = config.createSubConfig();
		config.set(name, sub);
		config.setComment(name, Stream.of(comments).collect(Collectors.joining(System.getProperty("line.separator"))));
		return new ConfigWrapper(file, sub);
	}
	
	public class ConfigValueBuilder<T> {
		
		static final BiPredicate<Object, Object> VALIDATOR = (raw, defaultValue) -> {
			System.out.println(raw + " --- " + defaultValue + " --- " + raw.getClass() + " --- " + defaultValue.getClass());
			return raw != null && defaultValue != null && defaultValue.getClass().isAssignableFrom(raw.getClass());
		};
		
		final String key;
		final T defaultValue;
		
		List<String> comments = new ArrayList<String>();
		Predicate<T> validator = (t) -> true;
		
		protected ConfigValueBuilder(String key, T defaultValue)
		{
			this.key = key;
			this.defaultValue = defaultValue;
		}
		
		public ConfigValueBuilder<T> setValidator(Predicate<T> validator)
		{
			if(!validator.test(defaultValue))
			{
				throw new IllegalArgumentException("Default value '" + defaultValue + "' for key '" + key + "' fails validation (default value class: " + defaultValue.getClass() + ")");
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
		
		public ConfigValueBuilder<T> comment(String comment)
		{
			// I like a little spacing
			this.comments.add(" " + comment);
			return this;
		}
		
		public ConfigValueBuilder<T> comment(String... comments)
		{
			Stream.of(comments).forEach(this::comment);
			return this;
		}
		
		public ConfigValue<T> build()
		{
			if(!config.contains(key))
			{
				config.set(key, defaultValue);
			}
			
			if(!comments.isEmpty())
			{
				config.setComment(key, comments.stream().collect(Collectors.joining(System.getProperty("line.separator"))));
			}
			
//			Predicate<T> nestedValidator = (value) ->
//			{
//				boolean nullChecks = value != null && defaultValue != null;
//				
//				return nullChecks && ((List.class.isAssignableFrom(value.getClass()) && List.class.isAssignableFrom(defaultValue.getClass())) || defaultValue.getClass().isInstance(value)) && validator.test((T) value);
//			};
			
			Predicate<Object> predicate = (o) -> VALIDATOR.test(o, defaultValue);
			spec.define(key, defaultValue, predicate);
			return new ConfigValue<T>(key, defaultValue, predicate);
		}
	}
	
	public class ConfigValue<T> {
		
		private final String key;
		private final T defaultValue;
		private Predicate<Object> validator;
		
		public ConfigValue(String key, T defaultValue, Predicate<Object> validator)
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
					
					// I think it already calls this
					// if(validator.test(value))
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
				
				System.out.println(spec.isCorrect(key, rawValue));
				
				// Pass to corrector
				throw new ParsingException("Invalid config value for key " + key + ": " + rawValue);
			} catch(ClassCastException | ParsingException e)
			{
				CensorCraft.LOGGER.warn("Configuration file {} is not correct. Correcting", file.getNioPath());
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
	}
}
