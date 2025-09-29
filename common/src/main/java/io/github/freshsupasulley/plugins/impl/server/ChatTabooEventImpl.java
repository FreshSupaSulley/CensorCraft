package io.github.freshsupasulley.plugins.impl.server;

import io.github.freshsupasulley.censorcraft.api.events.PluginPunishments;
import io.github.freshsupasulley.censorcraft.api.events.server.ChatTabooEvent;

import java.util.UUID;

public class ChatTabooEventImpl extends ServerEventImpl implements ChatTabooEvent {
	
	private PluginPunishments punishments;
	private UUID uuid;
	private Object component;
	
	public ChatTabooEventImpl(PluginPunishments punishments, UUID uuid, Object component)
	{
		this.punishments = punishments;
		this.uuid = uuid;
		this.component = component;
	}
	
	@Override
	public PluginPunishments getPunishments()
	{
		return punishments;
	}
	
	@Override
	public UUID getPlayer()
	{
		return uuid;
	}
	
	@Override
	public void setText(Object component)
	{
		this.component = component;
	}
	
	@Override
	public Object getText()
	{
		return component;
	}
}
