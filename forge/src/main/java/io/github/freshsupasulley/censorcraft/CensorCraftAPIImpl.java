package io.github.freshsupasulley.censorcraft;

import io.github.freshsupasulley.censorcraft.api.CensorCraftServerAPI;

public class CensorCraftAPIImpl implements CensorCraftServerAPI {
	
	public static final CensorCraftServerAPI INSTANCE = new CensorCraftAPIImpl();
	
	private CensorCraftAPIImpl()
	{
	}
	
	// this does nothing but just look prettier than CensorCraft.INSTANCE
	// im a fan :)
	public static CensorCraftServerAPI instance()
	{
		return INSTANCE;
	}
}
