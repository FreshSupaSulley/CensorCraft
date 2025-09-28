package io.github.freshsupasulley.censorcraft.api.events;

import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;

import java.util.function.Consumer;

/**
 * Register callbacks for CensorCraft events and punishments.
 */
public interface PluginRegistration {
	
	/**
	 * Registers an event.
	 *
	 * @param eventClass the class of the event you want to receive
	 * @param onEvent    the consumer that is called when the event was dispatched
	 * @param <T>        the event type
	 */
	<T extends Event> void registerEvent(Class<T> eventClass, Consumer<T> onEvent);
	
	/**
	 * Registers a new punishment type that will be added to the server config file for the server admin to manage.
	 *
	 * @param punishment the punishment class to add. It <b>MUST</b> have a no arg constructor.
	 */
	void registerPunishment(Class<? extends Punishment> punishment);
}
