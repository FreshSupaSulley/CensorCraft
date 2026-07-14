package io.github.freshsupasulley.censorcraft.api.punishments;

import com.electronwill.nightconfig.core.CommentedConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Punishment parameter that can be applied globally and on a per-punishment basis to override its global value.
 *
 * @param <T> type of the parameter (List, Boolean, etc.).
 */
public final class PunishmentParam<T> {
	
	@FunctionalInterface
	public interface TriConsumer<T, U, V> {
		
		void accept(T t, U u, V v);
	}
	
	private static final List<PunishmentParam<?>> ALL_PARAMS = new ArrayList<>();
	
	@SuppressWarnings("rawtypes")
	public static final PunishmentParam<List> TABOOS = new PunishmentParam<>(List.class, "taboos", List.of(""), true, (key, init, c) -> c.defineList("taboos", List.of("boom"), "List of forbidden words and phrases (case-insensitive)"));
	
	// Booleans
	public static final PunishmentParam<Boolean> CHAT_TABOOS = new PunishmentParam<>(Boolean.class, "chat_taboos", true, (key, init, c) -> c.define(key, init, "When someone is punished, send what the player said to chat"));
	public static final PunishmentParam<Boolean> ISOLATE_WORDS = new PunishmentParam<>(Boolean.class, "isolate_words", true, (key, init, c) -> c.define(key, init, "If true, only whole words are considered (surrounded by spaces or word boundaries). If false, partial matches are allowed (e.g., 'art' triggers punishment for 'start')"));
	public static final PunishmentParam<Boolean> MONITOR_CHAT = new PunishmentParam<>(Boolean.class, "monitor_chat", true, (key, init, c) -> c.define(key, init, "Punish players for sending taboos to chat"));
	public static final PunishmentParam<Boolean> PUNISH_EVERYONE = new PunishmentParam<>(Boolean.class, "punish_everyone", false, (key, init, c) -> c.define(key, init, "Saying a taboo will punish everyone", "Cooldowns only apply to the person who triggered the punishment"));
	
	// Numbers (don't use Doubles or Ints or shit trust)
	public static final PunishmentParam<Number> PUNISHMENT_COOLDOWN = new PunishmentParam<>(Number.class, "punishment_cooldown", 0D, (key, init, c) -> c.defineInRange(key, init.doubleValue(), 0D, Double.MAX_VALUE, "Delay (in seconds) before a player can be punished again", "Enter a number to at least 1 decimal place!"));
	
	private final Class<T> clazz;
	private final String key;
	private final T defaultValue;
	private final boolean generate;
	private final TriConsumer<String, T, IConfigFile> consumer;
	
	public static List<PunishmentParam<?>> values()
	{
		return ALL_PARAMS;
	}
	
	public PunishmentParam(Class<T> clazz, String key, T defaultValue, boolean generate, TriConsumer<String, T, IConfigFile> consumer)
	{
		ALL_PARAMS.add(this);
		this.clazz = clazz;
		this.key = key;
		this.defaultValue = defaultValue;
		this.generate = generate;
		this.consumer = consumer;
	}
	
	public PunishmentParam(Class<T> clazz, String key, T defaultValue, TriConsumer<String, T, IConfigFile> consumer)
	{
		this(clazz, key, defaultValue, false, consumer);
	}
	
	public void applyDefinitions(IConfigFile config)
	{
		consumer.accept(key, defaultValue, config);
	}
	
	public T getValue(CommentedConfig config, T fallback)
	{
		Object obj = config.getOptional(key).orElse(fallback);
		
		// Check if this shit is the right type
		if(!clazz.isInstance(obj))
		{
			System.err.println("This dumbass misconfigured their server-config (wrong type. Expected '{}', got '{}' (value: '{}')). Returning default value of '{}'" + " " + defaultValue.getClass() + " " + obj.getClass() + " " + obj + " " + defaultValue);
			return fallback;
		}
		
		//noinspection unchecked
		return (T) obj;
	}
	
	public T getDefaultValue()
	{
		return defaultValue;
	}
	
	/**
	 * Determines if we should write the default value of the setting in each punishment's config.
	 *
	 * @return true if we should generate this value on server config file generation
	 */
	public boolean generateForPunishment()
	{
		return generate;
	}
}
