package io.github.freshsupasulley.censorcraft.api;

import io.github.freshsupasulley.censorcraft.api.events.EventRegistration;

public interface CensorCraftPlugin {
	
	/**
	 * @return the ID of this plugin - Has to be unique
	 */
	String getPluginId();
	
	/**
	 * Register your events here.
	 * 
	 * <p>
	 * See {@link EventRegistration} for a basic example.
	 * </p>
	 * 
	 * @param registration {@link EventRegistration}
	 */
	void registerEvents(EventRegistration registration);
}