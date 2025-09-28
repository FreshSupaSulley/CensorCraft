package io.github.freshsupasulley.censorcraft.api.events.server;

import io.github.freshsupasulley.censorcraft.api.CensorCraftServerAPI;
import io.github.freshsupasulley.censorcraft.api.events.Event;

/**
 * An event that occurs on the server-side.
 */
public interface ServerEvent extends Event {
	
	/**
	 * Gets the {@link CensorCraftServerAPI}.
	 *
	 * @return {@link CensorCraftServerAPI} instance
	 */
	CensorCraftServerAPI getAPI();
}
