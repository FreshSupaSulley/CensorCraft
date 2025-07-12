package io.github.freshsupasulley.censorcraft.api.events;

import java.util.UUID;

/**
 * This event is emitted when the server detects a player spoke a taboo and will be punished.
 */
public interface PunishEvent extends ServerEvent {
	
	/**
	 * Punishes a player.
	 * 
	 * @param playerUUID {@link UUID} of the player
	 * @param taboo      taboo the player spoke to cause the punishment
	 */
	void punishPlayer(UUID playerUUID, String taboo);
}
