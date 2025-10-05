package io.github.freshsupasulley.censorcraft.common.plugins.impl.server;

import io.github.freshsupasulley.censorcraft.api.CensorCraftServerAPI;
import io.github.freshsupasulley.censorcraft.api.events.server.ServerEvent;
import io.github.freshsupasulley.censorcraft.common.plugins.impl.EventImpl;

public class ServerEventImpl extends EventImpl implements ServerEvent {
	
	@Override
	public CensorCraftServerAPI getAPI()
	{
		return CensorCraftServerAPIImpl.INSTANCE;
	}
}
