package io.github.freshsupasulley.censorcraft.api;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Handles basic interaction with CensorCraft on the server side.
 */
public interface CensorCraftServerAPI {
	
	/**
	 * Punishes a player through the standard punishment pipeline.
	 *
	 * <p>The server will first run each punishment's server-side {@link Punishment#punish(Object) punish} method, then
	 * send a packet to the player indicating to reset their rolling audio buffer which will also fire its client-side
	 * code.</p>
	 *
	 * <p>The map must have a non-null key, but the value can be <code>null</code> which indicates to not announce a
	 * message for that particular punishment. Keep in mind, this is only applicable when <code>chat_taboos</code> is
	 * enabled in the server config.</p>
	 *
	 * <p>If the map is empty, it will only reset the audio buffer on the client's machine and the
	 * <code>censorcraft</code> scoreboard will not increase the player's score.</p>
	 *
	 * @param player      the <code>net.minecraft.server.level</code> server player object
	 * @param punishments {@link Map} of punishments to the taboo said by the player
	 */
	void punish(Object player, Map<Punishment, @Nullable String> punishments);
	
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
	 * Gets the {@link CommentedFileConfig} that represents the server config file.
	 *
	 * <p>You can use this to make changes to the config programmatically and then save your changes to disk with
	 * {@link CommentedFileConfig#save()}. Keep in mind that each punishment's settings are their own
	 * {@link CommentedConfig} instances, and you should access those with {@link #getConfigPunishments()} instead.
	 * You'll still need to use this {@link CommentedFileConfig} instance to write your changes to the file.</p>
	 *
	 * @return {@link CommentedFileConfig} instance representing the server config file
	 */
	CommentedFileConfig getServerConfig();
}
