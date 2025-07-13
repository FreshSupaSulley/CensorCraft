package io.github.freshsupasulley.censorcraft.api.events.client;

import io.github.freshsupasulley.censorcraft.api.CensorCraftClientAPI;
import io.github.freshsupasulley.censorcraft.api.events.Event;

public interface ClientEvent extends Event {
	
	/**
	 * Gets the {@link CensorCraftClientAPI}.
	 * 
	 * @return {@link CensorCraftClientAPI} instance
	 */
	CensorCraftClientAPI getAPI();
}
