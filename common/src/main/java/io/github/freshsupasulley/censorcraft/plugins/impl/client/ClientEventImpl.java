package io.github.freshsupasulley.censorcraft.plugins.impl.client;

import io.github.freshsupasulley.censorcraft.api.CensorCraftClientAPI;
import io.github.freshsupasulley.censorcraft.api.events.client.ClientEvent;
import io.github.freshsupasulley.censorcraft.plugins.impl.EventImpl;

public class ClientEventImpl extends EventImpl implements ClientEvent {
	
	@Override
	public CensorCraftClientAPI getAPI()
	{
		return CensorCraftClientAPIImpl.INSTANCE;
	}
}
