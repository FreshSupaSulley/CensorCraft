package io.github.freshsupasulley.censorcraft.api;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;
import org.jetbrains.annotations.Nullable;

/**
 * Handles basic interaction with CensorCraft on the server side.
 */
public interface CensorCraftServerAPI {
	
	/**
	 * Punishes a player through the standard punishment pipeline, whether they said a taboo or not.
	 *
	 * <p>The server will first run the punishments server-side punishment code, then send a packet to the player
	 * indicating to reset their rolling audio buffer, which will finally fire the client-side code for all punishments
	 * if implemented.</p>
	 *
	 * @param player      the <code>net.minecraft.server.level</code> server player object
	 * @param taboo       taboo they said, or <code>null</code> to not announce a message (only applicable when
	 *                    <code>chat_taboos</code> is enabled in the server config)
	 * @param punishments list of punishments to invoke onto the player
	 * @throws RuntimeException if the punishment failed to execute on the server
	 */
	void punish(Object player, @Nullable String taboo, Punishment... punishments);
	
	/**
	 * Gets the server config instance responsible for setting global taboos, punishments, etc.
	 *
	 * @return server config instance
	 */
	CommentedFileConfig getServerConfig();
}
