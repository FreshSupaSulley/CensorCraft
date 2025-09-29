package io.github.freshsupasulley.censorcraft.api.events;

import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Wraps a {@link Map} of plugins to their {@link List} of punishments that are usually filtered by some criteria (such
 * as only enabled punishments).
 */
public final class PluginPunishments {
	
	private Map<String, List<Punishment>> map;
	
	/**
	 * Creates a new {@link PluginPunishments} that wraps the {@link Map} of plugins to their punishments.
	 *
	 * @param map {@link Map} of plugins to their punishments
	 */
	public PluginPunishments(Map<String, List<Punishment>> map)
	{
		this.map = map;
	}
	
	/**
	 * Convenience method that returns a collective {@link Stream} of all punishments defined in the map by flat mapping
	 * each plugin's punishment list into one.
	 *
	 * <p>Be careful, it's possible for two separate plugins to define their own punishments that happen to share the
	 * same ID.</p>
	 *
	 * @return {@link Stream} of all punishments separated from their plugins
	 */
	public Stream<Punishment> flatMappedStream()
	{
		return map.values().stream().flatMap(List::stream);
	}
	
	/**
	 * Gets the raw {@link Map} of plugins to their punishments.
	 *
	 * @return {@link Map} of plugins to their punishments
	 */
	public Map<String, List<Punishment>> getRaw()
	{
		return map;
	}
}
