package io.github.freshsupasulley.censorcraft.api.events;

import java.util.function.Consumer;

public interface EventRegistration {
	
	/**
	 * Registers an event.
	 *
	 * @param eventClass the class of the event you want to receive
	 * @param onPacket   the consumer that is called when the event was dispatched
	 * @param <T>        the event type
	 */
	<T extends Event> void registerEvent(Class<T> eventClass, Consumer<T> onPacket);
}
