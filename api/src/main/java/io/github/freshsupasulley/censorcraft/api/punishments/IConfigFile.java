package io.github.freshsupasulley.censorcraft.api.punishments;

import com.electronwill.nightconfig.core.ConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A custom config file interface.
 *
 * <p>KNOWN QUIRKS:</p>
 *
 * <p>
 * Pretty much exclusively use int and double, as that's what NightConfig likes. If not, the config will come back as
 * incorrect every reload.
 * </p>
 */
public interface IConfigFile {
	
	ConfigSpec getSpec();
	Map<String, List<String>> getComments();
	
	/**
	 * Convenience method to bundle defining a value and a comment.
	 *
	 * @param <E>      value type
	 * @param key      config key
	 * @param value    initial value
	 * @param comments config comments
	 */
	default <E> void define(String key, E value, String... comments)
	{
		getSpec().define(key, value);
		addComment(key, comments);
	}
	
	/**
	 * Convenience method to bundle defining a list and a comment.
	 *
	 * @param key      config key
	 * @param value    initial value
	 * @param comments config comments
	 * @param <E>      value type
	 */
	default <E> void defineList(String key, List<E> value, String... comments)
	{
		getSpec().defineList(key, value, predicate -> true);
		addComment(key, comments);
	}
	
	/**
	 * Convenience method to define a value within a range.
	 *
	 * @param <T>          {@link Comparable} type
	 * @param key          config key
	 * @param defaultValue initial value (must be within the range)
	 * @param min          minimum value
	 * @param max          maximum value
	 * @param comments     config comments
	 */
	default <T extends Comparable<? super T>> void defineInRange(String key, T defaultValue, T min, T max, String... comments)
	{
		Predicate<Object> validator = v ->
		{
			if(v == null)
				return false;
			
			@SuppressWarnings("unchecked")
			T val = (T) v;
			return min.compareTo(val) <= 0 && max.compareTo(val) >= 0;
		};
		
		if(!validator.test(defaultValue))
			throw new IllegalStateException("Default value fails range validation");
		
		// Range needs to go at the bottom
		Stream.concat(Stream.of(comments), Stream.of("Range: " + min + " ~ " + max)).forEach(comment -> addComment(key, comment));
		getSpec().define(key, defaultValue, validator);
	}
	
	/**
	 * Convenience method to add a multi-line comment.
	 *
	 * @param key      key
	 * @param comments list of comments
	 */
	default void addComment(String key, String... comments)
	{
		// I like a space between the pound and the comment
		getComments().computeIfAbsent(key, list -> new ArrayList<>(comments.length)).addAll(Stream.of(comments).map(comment -> " " + comment).toList());
	}
}
