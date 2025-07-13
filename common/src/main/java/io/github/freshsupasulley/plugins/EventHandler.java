package io.github.freshsupasulley.plugins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;

import io.github.freshsupasulley.censorcraft.api.events.Event;
import io.github.freshsupasulley.censorcraft.api.events.client.ClientPunishedEvent;
import io.github.freshsupasulley.plugins.impl.client.ClientPunishedEventImpl;

public class EventHandler {
	
	private Logger logger;
	private final Map<Class<? extends Event>, List<Consumer<? extends Event>>> events;
	
	private EventHandler(Logger logger, Map<Class<? extends Event>, List<Consumer<? extends Event>>> events)
	{
		this.logger = logger;
		this.events = events;
	}
	
	public void onClientReceivePunish(String... punishment)
	{
		dispatchEvent(ClientPunishedEvent.class, new ClientPunishedEventImpl(punishment));
	}
	
	private <T extends Event> boolean dispatchEvent(Class<? extends T> eventClass, T event)
	{
		List<Consumer<? extends Event>> events = this.events.get(eventClass);
		
		if(events == null)
		{
			return false;
		}
		
		for(Consumer<? extends Event> evt : events)
		{
			try
			{
				@SuppressWarnings("unchecked")
				Consumer<T> e = (Consumer<T>) evt;
				e.accept(event);
				
				if(event.isCancelled())
				{
					break;
				}
			} catch(Exception e)
			{
				logger.error("Failed to dispatch event '{}'", event.getClass().getSimpleName(), e);
			}
		}
		
		return event.isCancelled();
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