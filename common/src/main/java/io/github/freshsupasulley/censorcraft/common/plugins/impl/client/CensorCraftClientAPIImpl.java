package io.github.freshsupasulley.censorcraft.common.plugins.impl.client;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import io.github.freshsupasulley.censorcraft.api.CensorCraftClientAPI;

public class CensorCraftClientAPIImpl implements CensorCraftClientAPI {
	
	// Global instance
	public static CensorCraftClientAPI INSTANCE;
	
	// Begin instance variables
	private final CommentedFileConfig config;
	
	public CensorCraftClientAPIImpl(CommentedFileConfig config)
	{
		this.config = config;
	}
	
	@Override
	public CommentedFileConfig getClientConfig()
	{
		return config;
	}
}
