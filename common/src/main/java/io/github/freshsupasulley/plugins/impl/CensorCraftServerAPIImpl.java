package io.github.freshsupasulley.plugins.impl;

import io.github.freshsupasulley.censorcraft.api.CensorCraftServerAPI;

public class CensorCraftServerAPIImpl implements CensorCraftServerAPI {
	
	public static final CensorCraftServerAPI INSTANCE = new CensorCraftServerAPIImpl();
	
	private CensorCraftServerAPIImpl()
	{
	}
	
	public static CensorCraftServerAPI instance()
	{
		return INSTANCE;
	}
}
