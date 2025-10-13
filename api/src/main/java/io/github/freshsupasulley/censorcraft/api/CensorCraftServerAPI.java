package io.github.freshsupasulley.censorcraft.api;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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
	 * Gets the {@link java.util.List} of {@link Punishment} instances that each represent one array of tables in the
	 * server config file.
	 *
	 * <p>Each array of table represented as a punishment will be included in this list, regardless if the
	 * punishment is enabled or not. Multiple punishments of the same class may be returned if the server admin defined
	 * multiple array of tables. You can access the punishment's raw config at {@link Punishment#config}.</p>
	 *
	 * @return list of enabled punishments
	 */
	List<Punishment> getConfigPunishments();
	
	/**
	 * Gets the server config instance responsible for setting global taboos, punishments, etc.
	 *
	 * @return server config instance
	 */
	CommentedFileConfig getServerConfig();
}
