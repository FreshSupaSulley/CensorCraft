package io.github.freshsupasulley.censorcraft.api.events.server;

import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;

public interface ServerConfigEvent extends ServerEvent {
	
	@Override
	default boolean isCancellable()
	{
		return false;
	}
	
	/**
	 * Registers a new punishment type that will be added to the server config file for the server admin to manage.
	 * 
	 * @param punishment the punishment class to add. It must have a default, no arg constructor.
	 */
	void registerPunishment(Class<? extends Punishment> punishment);
}
