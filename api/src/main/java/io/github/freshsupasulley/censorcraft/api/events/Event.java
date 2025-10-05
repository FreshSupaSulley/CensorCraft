package io.github.freshsupasulley.censorcraft.api.events;

/**
 * Represents a CensorCraft event that plugins can listen to and sometimes cancel.
 */
public interface Event {
	
	/**
	 * Determines if this event can be cancelled.
	 *
	 * @return if this event can be cancelled
	 */
	default boolean isCancellable()
	{
		return true;
	}
	
	/**
	 * Cancels this event. Does nothing if the event isn't cancellable.
	 *
	 * @return if the event was actually cancelled
	 */
	boolean cancel();
	
	/**
	 * Returns if this event was cancelled.
	 *
	 * @return if the event was cancelled
	 */
	boolean isCancelled();
}
