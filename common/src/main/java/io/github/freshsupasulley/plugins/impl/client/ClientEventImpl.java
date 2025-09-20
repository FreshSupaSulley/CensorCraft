package io.github.freshsupasulley.plugins.impl.client;

import io.github.freshsupasulley.censorcraft.api.CensorCraftClientAPI;
import io.github.freshsupasulley.censorcraft.api.events.client.ClientEvent;
import io.github.freshsupasulley.plugins.impl.CensorCraftClientAPIImpl;
import io.github.freshsupasulley.plugins.impl.EventImpl;

public class ClientEventImpl extends EventImpl implements ClientEvent {
	
	@Override
	public CensorCraftClientAPI getAPI()
	{
		return CensorCraftClientAPIImpl.INSTANCE;
	}
}
