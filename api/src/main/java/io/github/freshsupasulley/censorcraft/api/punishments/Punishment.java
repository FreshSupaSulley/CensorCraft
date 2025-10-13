package io.github.freshsupasulley.censorcraft.api.punishments;

import com.electronwill.nightconfig.core.CommentedConfig;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Punishments can run on the server, the client, or both. They are configurable in the server config file.
 *
 * <p>All punishments <b>MUST</b> have a default, no-arg constructor!</p>
 *
 * <p>Extend this class to define your own punishments. There are helper methods here to define options for your
 * punishment that appear in the server config file.</p>
 *
 * <p>Punishments are serialized and sent to the client, meaning instance variables will be accessible for client-side
 * punishments, with the notable exception of <code>config</code> (see {@link #punishClientSide()}).</p>
 */
public abstract class Punishment implements Serializable {
	
	/** Server config that defines this punishment. This is <b>ONLY</b> accessible server-side. */
	public transient CommentedConfig config;
	
	/**
	 * Initializes a new {@link Punishment}.
	 */
	public Punishment()
	{
	}
	
	/**
	 * Creates a new instance of the punishment.
	 *
	 * <p>
	 * All punishments <b>MUST</b> have a default, no-arg constructor.
	 * </p>
	 *
	 * @param <T>   punishment class
	 * @param clazz punishment class
	 * @return new {@link Punishment} instance
	 * @throws RuntimeException if instantiating this class goes wrong
	 */
	public static <T extends Punishment> T newInstance(Class<T> clazz)
	{
		try
		{
			return clazz.getDeclaredConstructor().newInstance();
		} catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * The ID of this punishment. <b>Must be unique to this plugin</b>!
	 *
	 * <p>Used to write the name of the punishment to the server config file and distinguish punishments over the
	 * wire. An example could be <code>explosion</code>, <code>mob_effects</code>, etc.</p>
	 *
	 * @return unique name of this punishment
	 */
	public abstract String getId();
	
	/**
	 * Builds your punishment's section of the server config file. Use the parent's <code>define</code> methods to build
	 * it. Example:
	 *
	 * <pre>{@code
	 * defineInRange("explosion_radius", 5D, 0D, Double.MAX_VALUE);
	 * }</pre>
	 *
	 * <p>
	 * You can use {@link #config} in {@link #punish(Object)} to retrieve the server admin's settings of what you
	 * defined here.
	 * </p>
	 */
	protected abstract void buildConfig();
	
	/**
	 * Punishes the player for this punishment type.
	 *
	 * @param player the <code>net.minecraft.server.level</code> server player object
	 */
	public abstract void punish(Object player);
	
	/**
	 * Punishes the player for this punishment type <b>on the client-side</b> (on the punished-player's machine).
	 *
	 * <p>Most of the time, server-side punishments are sufficient (which is why implementing this method is
	 * optional).</p>
	 *
	 * <p>Punishments are serialized and sent to the client, but <code>config</code> is not. Attempting to read from
	 * the config on the client will raise a {@link NullPointerException}. If you want to send config settings from the
	 * server to the client, take out what you need and store them as instance variables in {@link #punish(Object)}.
	 * Those instance variables will be serialized and sent to the client, where you can use them in this method.
	 * <b>Only store serializable instance variables</b>! Unserializable instance variables will raise
	 * {@link java.io.NotSerializableException}.</p>
	 */
	public void punishClientSide()
	{
	}
	
	/**
	 * Internally used to build the punishment's default parameters.
	 *
	 * @param config {@link CommentedConfig} instance
	 */
	public final void buildConfig(CommentedConfig config)
	{
		// Just for building
		this.config = config;
		
		define("enable", initEnable());
		define("taboo", new ArrayList<>(List.of("")), "List of punishment-specific forbidden words and phrases (case-insensitive)");
		define("ignore_global_taboos", false, "Global taboos don't trigger this punishment");
		
		buildConfig();
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
		config.setComment(key, Stream.of(comments).map(comment -> " " + comment).collect(Collectors.joining(System.lineSeparator())));
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
	 * Checks if this punishment type is enabled by the server admin.
	 *
	 * @return true if this punishment's <code>enabled</code> flag is set to true
	 */
	public boolean isEnabled()
	{
		return config.get("enable");
	}
	
	/**
	 * Gets the list of punishment-specific taboos.
	 *
	 * @return {@link List} of taboos
	 */
	public List<String> getTaboos()
	{
		List<String> taboos = config.getOrElse("taboo", List.of()); // not sure if the orElse is necessary
		return taboos;
	}
	
	/**
	 * Checks if this punishment type ignores the <code>global_taboos</code> array.
	 *
	 * @return true if the punishment type ignores global taboos
	 */
	public boolean ignoresGlobalTaboos()
	{
		return config.get("ignore_global_taboos");
	}
	
	/**
	 * Checks if a taboo is in the punishment-specific taboo trie and returns it if found. Can be <code>null</code>!
	 *
	 * <p>You can override this method to add customized taboos that can change at your whim.</p>
	 *
	 * @param sample       word or phrase the player said
	 * @param isolateWords if the server admin enabled isolate words in the config file
	 * @return the taboo the player spoke, or null if not found
	 */
	public @Nullable String getTaboo(String sample, boolean isolateWords)
	{
		// We can't store Tries as instance variables anymore so this is required
		Trie trie = new Trie(getTaboos());
		return isolateWords ? trie.containsAnyIsolatedIgnoreCase(sample) : trie.containsAnyIgnoreCase(sample);
	}
}
