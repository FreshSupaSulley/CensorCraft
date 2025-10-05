package io.github.freshsupasulley.censorcraft.api;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import de.maxhenkel.voicechat.api.ServerPlayer;
import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;
import org.jetbrains.annotations.Nullable;

/**
 * Handles basic interaction with CensorCraft on the server side.
 */
public interface CensorCraftServerAPI {
	
	/**
	 * Punishes a player, whether they said a taboo or not.
	 *
	 * @param player      {@link ServerPlayer} object
	 * @param taboo       taboo they said, or <code>null</code> to not announce a message (only applicable when
	 *                    <code>chat_taboos</code> is enabled in the server config)
	 * @param punishments list of punishments to invoke onto the player
	 */
	void punish(ServerPlayer player, @Nullable String taboo, Punishment... punishments);
	
	/**
	 * Gets the server config instance responsible for setting global taboos, punishments, etc.
	 *
	 * @return server config instance
	 */
	CommentedFileConfig getServerConfig();
}
