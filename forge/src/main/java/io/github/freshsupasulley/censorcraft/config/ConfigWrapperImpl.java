package io.github.freshsupasulley.censorcraft.config;

import com.electronwill.nightconfig.core.CommentedConfig;

import io.github.freshsupasulley.censorcraft.api.punishments.ConfigWrapper;

public class ConfigWrapperImpl implements ConfigWrapper {
	
	private CommentedConfig config;
	
	public ConfigWrapperImpl(CommentedConfig config)
	{
		this.config = config;
	}
	
	@Override
	public <T> T get(String key)
	{
		return config.get(key);
	}
	
	@Override
	public <T> void set(String key, T value)
	{
		config.set(key, value);
	}
	
	@Override
	public void setComment(String key, String comment)
	{
		config.setComment(key, comment);
	}
	
	@Override
	public int getInt(String key)
	{
		return config.getInt(key);
	}
	
	@Override
	public <T extends Enum<T>> T getEnum(String path, Class<T> enumType)
	{
		return config.getEnum(path, enumType);
	}
	
}
