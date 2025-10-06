package io.github.freshsupasulley.censorcraft.plugins;

import io.github.freshsupasulley.censorcraft.api.events.Event;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public record EventHandler(Logger logger, Map<Class<? extends Event>, List<Consumer<? extends Event>>> events) {
	
	/**
	 * Dispatches an event to all CensorCraft plugins.
	 *
	 * @param <T>        event class
	 * @param eventClass event class
	 * @param event      event instance
	 * @return true if the event was fired, false if cancelled
	 */
	public <T extends Event> boolean dispatchEvent(Class<? extends T> eventClass, T event)
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
					logger.debug("{} event was cancelled", eventClass.getSimpleName());
					break;
				}
			} catch(Exception e)
			{
				logger.error("Something went wrong dispatching event {}", eventClass.getSimpleName(), e);
			}
		}
		
		return !event.isCancelled();
	}
	
	public static class EventHandlerBuilder {
		
		private final Logger logger;
		private final Map<Class<? extends Event>, List<Consumer<? extends Event>>> events;
		
		public EventHandlerBuilder(Logger logger)
		{
			this.logger = logger;
			events = new HashMap<>();
		}
		
		public <T extends Event> void addEvent(Class<T> eventClass, Consumer<T> event)
		{
			List<Consumer<? extends Event>> eventList = this.events.getOrDefault(eventClass, new ArrayList<>());
			eventList.add(event);
			this.events.put(eventClass, eventList);
		}
		
		public EventHandler build()
		{
			return new EventHandler(logger, events);
		}
	}
}