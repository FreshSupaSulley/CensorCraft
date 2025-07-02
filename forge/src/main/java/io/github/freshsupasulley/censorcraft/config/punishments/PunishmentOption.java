package io.github.freshsupasulley.censorcraft.config.punishments;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.electronwill.nightconfig.core.CommentedConfig;

import io.github.freshsupasulley.censorcraft.network.Trie;
import net.minecraft.server.level.ServerPlayer;

public abstract class PunishmentOption<T extends PunishmentOption<T>> {
	
	protected CommentedConfig config;
	private Trie tabooTrie;
	
	public void init(boolean initEnable, CommentedConfig config)
	{
		config.set("enable", initEnable);
		
		config.set("taboo", new ArrayList<>(List.of("")));
		config.setComment("taboo", "List of punishment-specific forbidden words and phrases (case-insensitive)");
		tabooTrie = new Trie(List.of()); // Will be updated when getTaboo is called
		
		config.set("ignore_global_taboos", false);
		config.setComment("ignore_global_taboos", "Global taboos don't trigger this punishment");
		
		build(config);
	}
	
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
		config.set(key, value);
		config.setComment(key, Stream.of(comments).collect(Collectors.joining(System.getProperty("line.separator"))));
	}
	
	/**
	 * Convenience method to bundle defining a value and a comment for an enum.
	 * 
	 * @param <E>      {@link Enum} type
	 * @param key      config key
	 * @param value    initial value (enum entry)
	 * @param comments config comments
	 */
	final <E extends Enum<E>> void define(String key, E value, String... comments)
	{
		// holy UGLYYYY
		define(key, value, Stream.concat(Stream.of(comments), Stream.of("Allowed Values: " + Stream.of(value.getDeclaringClass().getEnumConstants()).map(Enum::name).collect(Collectors.joining(", ")))).toArray(String[]::new));
	}
	
	/**
	 * Convenience method to define a value within a range. <b>There is no error checking!</b>
	 * 
	 * @param <E>      {@link Comparable} type
	 * @param key      config key
	 * @param value    initial value (must be within the range)
	 * @param min      minimum value
	 * @param max      maximum value
	 * @param comments config comments
	 */
	final <E extends Comparable<? super E>> void defineInRange(String key, E value, E min, E max, String... comments)
	{
		define(key, value, Stream.concat(Stream.of("Range: " + min + " ~ " + max), Stream.of(comments)).toArray(String[]::new));
	}
	
	abstract void build(CommentedConfig config);
	
	abstract T newInstance();
	
	public final T deserialize(CommentedConfig config)
	{
		this.config = config;
		return newInstance();
	}
	
	public final boolean isEnabled()
	{
		return config.get("enable");
	}
	
	public final boolean ignoresGlobalTaboos()
	{
		return config.get("ignore_global_taboos");
	}
	
	public final String getTaboo(String word, boolean isolateWords)
	{
		// Update trie in case the taboos did
		tabooTrie.update(config.get("taboo"));
		return isolateWords ? tabooTrie.containsAnyIsolatedIgnoreCase(word) : tabooTrie.containsAnyIgnoreCase(word);
	}
	
	public abstract void punish(ServerPlayer player);
	
	// Can be overriden
	public String getName()
	{
		return this.getClass().getSimpleName().toLowerCase();
	}
	
	public String getDescription()
	{
		return "";
	}
}
