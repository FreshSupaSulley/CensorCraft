package io.github.freshsupasulley.plugins.impl.server;

import java.util.function.Consumer;

import io.github.freshsupasulley.censorcraft.api.events.server.ServerConfigEvent;
import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;

public class ServerConfigEventImpl extends ServerEventImpl implements ServerConfigEvent {
	
	private Consumer<Class<? extends Punishment>> callback;
	
	public ServerConfigEventImpl(Consumer<Class<? extends Punishment>> callback)
	{
		this.callback = callback;
	}
	
	@Override
	public void registerPunishment(Class<? extends Punishment> punishment)
	{
		callback.accept(punishment);
	}
}
