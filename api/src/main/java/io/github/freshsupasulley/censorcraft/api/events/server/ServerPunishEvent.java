package io.github.freshsupasulley.censorcraft.api.events.server;

import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;

import java.util.UUID;

/**
 * Fires when the player is about to be punished on the server-side.
 */
public interface ServerPunishEvent extends ServerEvent {
	
	/**
	 * Gets the player UUID who's about to be punished.
	 *
	 * @return player {@link UUID}
	 */
	UUID getPlayerUUID();
	
	/**
	 * Gets the {@link Punishment} this player triggered.
	 *
	 * @return {@link Punishment} instance
	 */
	Punishment getPunishment();
}
