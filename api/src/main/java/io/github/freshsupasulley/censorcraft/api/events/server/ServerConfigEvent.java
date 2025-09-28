package io.github.freshsupasulley.censorcraft.api.events.server;

/**
 * Fires when the server is about to start.
 *
 * <p>Completion of this event means the server config file was created.</p>
 */
public interface ServerConfigEvent extends ServerEvent {
	
	@Override
	default boolean isCancellable()
	{
		return false;
	}
}
