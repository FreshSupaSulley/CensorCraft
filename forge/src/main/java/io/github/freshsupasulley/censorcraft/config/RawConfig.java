package io.github.freshsupasulley.censorcraft.config;

import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileNotFoundAction;
import com.electronwill.nightconfig.core.io.WritingMode;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import net.minecraftforge.fml.loading.FMLPaths;

public abstract class RawConfig {
	
	private CommentedFileConfig config;
	
	public RawConfig(String name)
	{
		config = CommentedFileConfig.builder(FMLPaths.CONFIGDIR.get().resolve(CensorCraft.MODID + name + ".toml")).autosave().autoreload().writingMode(WritingMode.REPLACE).sync().preserveInsertionOrder().onFileNotFound(FileNotFoundAction.CREATE_EMPTY).build();
		config.load();
		
		register();
		
		config.save();
	}
	
	protected abstract void register();
	
	// <T> ConfigValue<T> setDefault(String key, T defaultValue, String comment, Predicate<T> validator)
	// {
	// if(!config.contains(key))
	// {
	// config.set(key, defaultValue);
	// }
	//
	// config.setComment(key, comment);
	//
	// return new ConfigValue<T>(config, key, defaultValue, validator, comment);
	// }
	
	<T> ConfigValue<T> add(ConfigValueBuilder<T> builder)
	{
		ConfigValue<T> built = builder.build(config);
		
		if(!config.contains(built.key))
		{
			config.set(built.key, built.defaultValue);
		}
		
		if(built.comment != null)
		{
			config.setComment(built.key, built.comment);
		}
		
		return built;
	}
	
	class ConfigValueBuilder<T> {
		
		private final String key;
		private final T defaultValue;
		
		// Optional
		private String comment;
		private Predicate<T> validator = (t) -> true;
		
		public ConfigValueBuilder(String key, T defaultValue)
		{
			this.key = key;
			this.defaultValue = defaultValue;
		}
		
		public ConfigValueBuilder<T> addValidator(Predicate<T> validator)
		{
			this.validator = validator;
			return this;
		}
		
		public ConfigValueBuilder<T> addComment(String comment)
		{
			this.comment = comment;
			return this;
		}
		
		public ConfigValue<T> build(CommentedFileConfig config)
		{
			return new ConfigValue<T>(config, key, defaultValue, validator, comment);
		}
	}
	
	class ConfigValue<T> {
		
		private final CommentedFileConfig config;
		private final String key;
		private final T defaultValue;
		private Predicate<T> validator;
		private String comment;
		
		public ConfigValue(CommentedFileConfig config, String key, T defaultValue, Predicate<T> validator, @Nullable String comment)
		{
			this.config = config;
			this.key = key;
			this.defaultValue = defaultValue;
			this.validator = validator;
			this.comment = comment;
		}
		
		public T get()
		{
			T value = config.getOrElse(key, defaultValue);
			
			if(validator.test(defaultValue))
			{
				return value;
			}
			
			return defaultValue;
		}
		
		public void set(T value)
		{
			if(validator.test(value))
			{
				config.set(key, value);
			}
		}
		
		public String getKey()
		{
			return key;
		}
	}
}
