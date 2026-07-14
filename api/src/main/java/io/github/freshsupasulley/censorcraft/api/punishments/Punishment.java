package io.github.freshsupasulley.censorcraft.api.punishments;

import com.electronwill.nightconfig.core.CommentedConfig;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Optional;
import java.util.Set;
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
 * punishments, with the notable exception of <code>config</code> (see {@link #punishClientSide(Set)}).</p>
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
	 * Internally used to set the {@link #config} local variable and define the <code>enabled</code> parameter.
	 *
	 * @param config {@link CommentedConfig} instance
	 */
	public final void initialize(CommentedConfig config)
	{
		this.config = config;
		define("enabled", isEnabledOnInit());
		buildConfig();
	}
	
	/**
	 * Checks if this punishment type is enabled by the server admin.
	 *
	 * @return true if this punishment's <code>enable</code> flag is set to true
	 */
	public final boolean isEnabled()
	{
		// Definitely default to false if we can't find it
		return config.getOrElse("enabled", false);
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
	 * Builds your punishment's section of the server config file. Your punishment may not need configuration from the
	 * admin, and if so you may leave this method unimplemented.
	 *
	 * <p>
	 * Use the parent's {@code define} methods to build it. Example:
	 *
	 * <pre>{@code
	 * defineInRange("explosion_radius", 5D, 0D, Double.MAX_VALUE);
	 * }</pre>
	 *
	 * <p>
	 * You can use {@link #config} in {@link #punish(Object, Set)} to retrieve the server config's values of what you
	 * defined here.
	 */
	protected abstract void buildConfig();
	
	/**
	 * Returns the value of a punishment's parameter if it was set.
	 *
	 * <p>This method exists so plugins can override this and ignore server config settings. Useful if you wanted the
	 * taboos to always be a particular set of words, for example.</p>
	 *
	 * @param param    parameter to check
	 * @param fallback what to return instead if the punishment doesn't have this set (the server config global
	 *                 setting)
	 * @param <T>      type of global param
	 * @return {@link Optional} global punishment parameter
	 */
	public <T> T getParam(PunishmentParam<T> param, T fallback)
	{
		return param.getValue(config, fallback);
	}
	
	/**
	 * Punishes the player for this punishment type.
	 *
	 * <p>This method returns a {@link PunishmentJob}. If your punishment takes long to run, consider using it to
	 * spread your compute across multiple ticks. Otherwise, you may run all of your punishment code in this method and
	 * return <code>null</code>.</p>
	 *
	 * @param player the <code>net.minecraft.server.level</code> server player object
	 * @param taboos the taboos the player said
	 * @return optional {@link PunishmentJob} to queue a large job
	 */
	public abstract @Nullable PunishmentJob punish(Object player, Set<String> taboos);
	
	/**
	 * Punishes the player for this punishment type <b>on the client-side</b> (on the punished-player's machine).
	 *
	 * <p>Most of the time, server-side punishments are sufficient (which is why implementing this method is
	 * optional).</p>
	 *
	 * <p>Punishments are serialized and sent to the client, but <code>config</code> is not. Attempting to read from
	 * the config on the client will raise a {@link NullPointerException}. If you want to send config settings from the
	 * server to the client, take out what you need and store them as instance variables in
	 * {@link #punish(Object, Set)}. Those instance variables will be serialized and sent to the client, where you can
	 * use them in this method.
	 * <b>Only store serializable instance variables</b>! Unserializable instance variables will raise
	 * {@link java.io.NotSerializableException}.</p>
	 *
	 * @param taboos the taboos the player said
	 */
	public void punishClientSide(Set<String> taboos)
	{
	}
	
	/**
	 * Convenience method to bundle defining a value and a comment.
	 *
	 * @param <E>      value type
	 * @param key      config key
	 * @param value    initial value
	 * @param comments config comments
	 */
	public final <E> void define(String key, E value, String... comments)
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
	public final <E extends Enum<E>> void defineEnum(String key, E value, String... comments)
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
	public final <E extends Comparable<? super E>> void defineInRange(String key, E value, E min, E max, String... comments)
	{
		define(key, value, Stream.concat(Stream.of(comments), Stream.of("Range: " + min + " ~ " + max)).toArray(String[]::new));
	}
	
	/**
	 * Whether the punishment will be enabled by default when the server config file is first created for a world.
	 *
	 * @return true if this punishment should be enabled by default when generated
	 */
	protected boolean isEnabledOnInit()
	{
		return false;
	}
}
