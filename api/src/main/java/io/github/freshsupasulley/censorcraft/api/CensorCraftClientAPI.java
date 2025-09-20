package io.github.freshsupasulley.censorcraft.api;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;

/**
 * Handles basic interaction with CensorCraft on the client side.
 */
public interface CensorCraftClientAPI {
	
	/**
	 * Gets the client config instance responsible for setting client-side options.
	 *
	 * @return client config instance
	 */
	CommentedFileConfig getClientConfig();
}
