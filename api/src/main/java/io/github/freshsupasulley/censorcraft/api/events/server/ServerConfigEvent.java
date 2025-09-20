package io.github.freshsupasulley.censorcraft.api.events.server;

public interface ServerConfigEvent extends ServerEvent {
	
	@Override
	default boolean isCancellable()
	{
		return false;
	}
}
