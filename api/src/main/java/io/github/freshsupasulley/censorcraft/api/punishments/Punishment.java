package io.github.freshsupasulley.censorcraft.api.punishments;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

/**
 * Defines a punishment type.
 * 
 * @param <T> self-referential generic type (set the generic to the same class). Used to deserialize from the config file.
 */
public abstract class Punishment<T extends Punishment<T>> {
	
	protected ConfigWrapper config;
	
	/**
	 * Internally used to build the punishment's default parameters.
	 * 
	 * @param config {@link ConfigWrapper} instance
	 */
	public final void fillConfig(ConfigWrapper config)
	{
		// Just for building
		this.config = config;
		
		define("enable", initEnable());
		define("taboo", new ArrayList<>(List.of("")), "List of punishment-specific forbidden words and phrases (case-insensitive)");
		define("ignore_global_taboos", false, "Global taboos don't trigger this punishment");
		
		build();
	}
	
	/**
	 * Convenience method to bundle defining a value and a comment.
	 * 
	 * @param <E>      value type
	 * @param key      config key
	 * @param value    initial value
	 * @param comments config comments
	 */
	protected final <E> void define(String key, E value, String... comments)
	{
		config.set(key, value);
		config.setComment(key, Stream.of(comments).map(comment -> " " + comment).collect(Collectors.joining(System.getProperty("line.separator"))));
	}
	
	/**
	 * Convenience method to bundle defining a value and a comment for an enum.
	 * 
	 * @param <E>      {@link Enum} type
	 * @param key      config key
	 * @param value    initial value (enum entry)
	 * @param comments config comments
	 */
	protected final <E extends Enum<E>> void defineEnum(String key, E value, String... comments)
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
	protected final <E extends Comparable<? super E>> void defineInRange(String key, E value, E min, E max, String... comments)
	{
		define(key, value, Stream.concat(Stream.of(comments), Stream.of("Range: " + min + " ~ " + max)).toArray(String[]::new));
	}
	
	/**
	 * Whether the punishment will be enabled by default when written to the server config file.
	 * 
	 * @return true if this punishment should be enabled by default
	 */
	protected boolean initEnable()
	{
		return false;
	}
	
	/**
	 * Builds your punishment's section of the server config file. Use the parent's <code>define</code> methods to build it.
	 * 
	 * <h3>Example:</h3>
	 * 
	 * <pre>{@code
	 * defineInRange("explosion_radius", 5D, 0D, Double.MAX_VALUE);
	 * }</pre>
	 * 
	 * <p>
	 * You may later use {@link #config} in {@link #punish(UUID)} to retrieve the server admin's settings of what you defined here.
	 */
	protected abstract void build();
	
	/**
	 * Create a dummy punishment instance (used for deserialization).
	 * 
	 * @return new punishment instance of this type
	 */
	protected abstract T newInstance();
	
	/**
	 * Deserializes this punishment type from the server config file.
	 * 
	 * @param config {@link ConfigWrapper} instance
	 * @return new punishment instance
	 */
	public final T deserialize(ConfigWrapper config)
	{
		T option = newInstance();
		option.config = config;
		return option;
	}
	
	/**
	 * Checks if this punishment type is enabled by the server admin.
	 * 
	 * @return true if this punishment's <code>enabled</code> flag is set to true
	 */
	public final boolean isEnabled()
	{
		return config.get("enable");
	}
	
	/**
	 * Checks if this punishment type ignores the <code>global_taboos</code> array.
	 * 
	 * @return true if the punishment type ignores global taboos
	 */
	public final boolean ignoresGlobalTaboos()
	{
		return config.get("ignore_global_taboos");
	}
	
	/**
	 * Checks a sample word (what a player said) to the taboo trie and returns a taboo if the player said one.
	 * 
	 * @param sample       word or phrase the player said
	 * @param isolateWords if the server admin enabled isolate words in the config file
	 * @return the taboo the player spoke, if any
	 */
	@Nullable
	public final String getTaboo(String sample, boolean isolateWords)
	{
		// We can't store Tries as instance variables anymore so this is required
		List<String> taboos = config.get("taboo");
		Trie trie = new Trie(taboos);
		return isolateWords ? trie.containsAnyIsolatedIgnoreCase(sample) : trie.containsAnyIgnoreCase(sample);
	}
	
	/**
	 * Punishes the player for this punishment type.
	 * 
	 * <p>
	 * You'll need to get the server level yourself to change anything to the world!
	 * </p>
	 * 
	 * @param player {@link UUID} UUID of the server player
	 */
	public abstract void punish(UUID player);
	
	/**
	 * Used to write the name of the punishment to the server config file. By default, the name is derived from the class name.
	 * 
	 * @return name of the punishment type
	 */
	public String getName()
	{
		return this.getClass().getSimpleName().toLowerCase();
	}
}
