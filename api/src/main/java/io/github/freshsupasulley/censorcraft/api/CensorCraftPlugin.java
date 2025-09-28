package io.github.freshsupasulley.censorcraft.api;

import io.github.freshsupasulley.censorcraft.api.events.PluginRegistration;

/**
 * Entrypoint for defining a CensorCraft plugin.
 */
public interface CensorCraftPlugin {
	
	/**
	 * Get the unique ID of the plugin. Cannot conflict with another plugin.
	 *
	 * @return the ID of this plugin
	 */
	String getPluginId();
	
	/**
	 * Register your events and punishments here.
	 *
	 * @param registration {@link PluginRegistration}
	 */
	void register(PluginRegistration registration);
}