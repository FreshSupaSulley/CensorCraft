package io.github.freshsupasulley.plugins.impl;

import io.github.freshsupasulley.censorcraft.api.CensorCraftClientAPI;

public class CensorCraftClientAPIImpl implements CensorCraftClientAPI {
	
	public static final CensorCraftClientAPI INSTANCE = new CensorCraftClientAPIImpl();
	
	private CensorCraftClientAPIImpl()
	{
	}
	
	public static CensorCraftClientAPI instance()
	{
		return INSTANCE;
	}
}
