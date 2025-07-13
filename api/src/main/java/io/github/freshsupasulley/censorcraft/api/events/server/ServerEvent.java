package io.github.freshsupasulley.censorcraft.api.events.server;

import io.github.freshsupasulley.censorcraft.api.CensorCraftServerAPI;
import io.github.freshsupasulley.censorcraft.api.events.Event;

public interface ServerEvent extends Event {
	
	/**
	 * Gets the {@link CensorCraftServerAPI}.
	 * 
	 * @return {@link CensorCraftServerAPI} instance
	 */
	CensorCraftServerAPI getAPI();
}
