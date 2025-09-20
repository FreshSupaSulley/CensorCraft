package io.github.freshsupasulley.censorcraft.api;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;

/**
 * Handles basic interaction with CensorCraft on the server side.
 */
public interface CensorCraftServerAPI {
	
	/**
	 * Gets the server config instance responsible for setting global taboos, punishments, etc.
	 *
	 * @return server config instance
	 */
	CommentedFileConfig getServerConfig();
}
