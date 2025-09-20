package io.github.freshsupasulley.censorcraft.api.events.server;

/**
 * Fires when the server config file has been created, which fires when the server is about to start.
 */
public interface ServerConfigEvent extends ServerEvent {
	
	@Override
	default boolean isCancellable()
	{
		return false;
	}
}
