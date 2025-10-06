package io.github.freshsupasulley.censorcraft.plugins.impl.server;

import io.github.freshsupasulley.censorcraft.api.events.server.ChatTabooEvent;
import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;

import java.util.List;
import java.util.UUID;

public class ChatTabooEventImpl extends ServerEventImpl implements ChatTabooEvent {
	
	private List<Punishment> punishments;
	private UUID uuid;
	private Object component;
	
	public ChatTabooEventImpl(List<Punishment> punishments, UUID uuid, Object component)
	{
		this.punishments = punishments;
		this.uuid = uuid;
		this.component = component;
	}
	
	@Override
	public List<Punishment> getPunishments()
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
