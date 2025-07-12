package io.github.freshsupasulley.censorcraft.api;

import io.github.freshsupasulley.censorcraft.api.events.EventRegistration;

public interface CensorCraftPlugin {
	
	/**
	 * @return the ID of this plugin - Has to be unique
	 */
	String getPluginId();
	
	/**
	 * Called after loading the plugin.
	 */
	default void initialize(EventRegistration registration)
	{
	}
}