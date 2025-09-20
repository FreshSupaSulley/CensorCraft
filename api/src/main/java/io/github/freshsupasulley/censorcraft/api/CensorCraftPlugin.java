package io.github.freshsupasulley.censorcraft.api;

import io.github.freshsupasulley.censorcraft.api.events.PluginRegistration;

/**
 * Entrypoint for defining a CensorCraft plugin.
 */
public interface CensorCraftPlugin {
	
	/**
	 * @return the ID of this plugin - probably needs to be unique?
	 */
	String getPluginId();
	
	/**
	 * Register your events and punishments here.
	 *
	 * @param registration {@link PluginRegistration}
	 */
	void register(PluginRegistration registration);
}