package io.github.freshsupasulley.censorcraft;

import io.github.freshsupasulley.censorcraft.api.CensorCraftAPI;
import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;
import io.github.freshsupasulley.censorcraft.config.ServerConfig;

public class CensorCraftAPIImpl implements CensorCraftAPI {
	
	public static final CensorCraftAPI INSTANCE = new CensorCraftAPIImpl();
	
	private CensorCraftAPIImpl()
	{
	}
	
	// this does nothing but just look prettier than CensorCraft.INSTANCE
	// im a fan :)
	public static CensorCraftAPI instance()
	{
		return INSTANCE;
	}
	
	@Override
	public void registerPunishment(Punishment punishment)
	{
		ServerConfig.get().registerPunishment(punishment);
	}
}
