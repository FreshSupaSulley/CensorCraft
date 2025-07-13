package io.github.freshsupasulley.censorcraft.api.punishments;

/**
 * An incredibly basic config wrapper for writing to config files.
 */
public interface ConfigWrapper {
	
	public <T> T get(String key);
	
	public <T> void set(String key, T value);
	
	public void setComment(String key, String comment);
	
	public int getInt(String key);
	
	public <T extends Enum<T>> T getEnum(String key, Class<T> enumType);
}
