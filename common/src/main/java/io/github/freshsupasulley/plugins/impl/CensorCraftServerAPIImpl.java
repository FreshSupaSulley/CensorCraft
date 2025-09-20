package io.github.freshsupasulley.plugins.impl;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import io.github.freshsupasulley.censorcraft.api.CensorCraftServerAPI;

public class CensorCraftServerAPIImpl implements CensorCraftServerAPI {
	
	// Instance everyone can see
	public static CensorCraftServerAPI INSTANCE;
	
	// Begin instance variables
	private final CommentedFileConfig config;
	
	public CensorCraftServerAPIImpl(CommentedFileConfig config)
	{
		this.config = config;
	}
	
	public CommentedFileConfig getServerConfig()
	{
		return config;
	}
}
