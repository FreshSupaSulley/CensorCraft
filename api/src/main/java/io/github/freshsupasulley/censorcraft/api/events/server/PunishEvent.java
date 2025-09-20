package io.github.freshsupasulley.censorcraft.api.events.server;

import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;

import java.util.UUID;

/**
 * Server side event that fires when the player is punished.
 */
public interface PunishEvent extends ServerEvent {
	
	/**
	 * Gets the player UUID who's about to be punished.
	 *
	 * @return player {@link UUID}
	 */
	UUID getPlayerUUID();
	
	/**
	 * Gets the punishment this player triggered.
	 *
	 * @return {@link Punishment} object
	 */
	Punishment getPunishments();
}
