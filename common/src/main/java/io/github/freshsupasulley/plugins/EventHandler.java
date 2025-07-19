package io.github.freshsupasulley.plugins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;

import io.github.freshsupasulley.censorcraft.api.events.Event;
import io.github.freshsupasulley.censorcraft.api.events.client.ClientAcknowledgePunish;
import io.github.freshsupasulley.censorcraft.api.events.server.PunishEvent;
import io.github.freshsupasulley.censorcraft.api.events.server.ServerConfigEvent;
import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;
import io.github.freshsupasulley.plugins.impl.client.ClientAcknowledgePunishImpl;
import io.github.freshsupasulley.plugins.impl.server.PunishEventImpl;
import io.github.freshsupasulley.plugins.impl.server.ServerConfigEventImpl;

public class EventHandler {
	
	private Logger logger;
	private final Map<Class<? extends Event>, List<Consumer<? extends Event>>> events;
	
	private EventHandler(Logger logger, Map<Class<? extends Event>, List<Consumer<? extends Event>>> events)
	{
		this.logger = logger;
		this.events = events;
	}
	
	public void onServerConfig(Consumer<Class<? extends Punishment>> consumer)
	{
		dispatchEvent(ServerConfigEvent.class, new ServerConfigEventImpl(consumer));
	}
	
	public boolean onPunish(Punishment punishments)
	{
		return dispatchEvent(PunishEvent.class, new PunishEventImpl(punishments));
	}
	
	public void onClientReceivePunish(String... punishment)
	{
		dispatchEvent(ClientAcknowledgePunish.class, new ClientAcknowledgePunishImpl(punishment));
	}
	
	/**
	 * Dispatches an event to all CensorCraft plugins.
	 * 
	 * @param <T>        event class
	 * @param eventClass event class
	 * @param event      event instance
	 * @return true if the event was fired, false if cancelled
	 */
	private <T extends Event> boolean dispatchEvent(Class<? extends T> eventClass, T event)
	{
		List<Consumer<? extends Event>> events = this.events.get(eventClass);
		
		if(events == null)
		{
			// Event can't be cancelled
			return true;
		}
		
		for(Consumer<? extends Event> sample : events)
		{
			try
			{
				@SuppressWarnings("unchecked")
				Consumer<T> e = (Consumer<T>) sample;
				e.accept(event);
				
				if(event.isCancelled())
				{
					logger.debug("'{}' event was cancelled", event.getClass().getSimpleName());
					break;
				}
			} catch(Exception e)
			{
				logger.error("Failed to dispatch event '{}'", event.getClass().getSimpleName(), e);
			}
		}
		
		return !event.isCancelled();
	}
	
	public static class EventHandlerBuilder {
		
		private Logger logger;
		private final Map<Class<? extends Event>, List<Consumer<? extends Event>>> events;
		
		public EventHandlerBuilder(Logger logger)
		{
			this.logger = logger;
			events = new HashMap<>();
		}
		
		public <T extends Event> EventHandlerBuilder addEvent(Class<T> eventClass, Consumer<T> event)
		{
			List<Consumer<? extends Event>> eventList = this.events.getOrDefault(eventClass, new ArrayList<>());
			eventList.add(event);
			this.events.put(eventClass, eventList);
			return this;
		}
		
		public EventHandler build()
		{
			return new EventHandler(logger, events);
		}
	}
}