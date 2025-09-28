package io.github.freshsupasulley.censorcraft.api.events.server;

import io.github.freshsupasulley.censorcraft.api.punishments.ServerPunishment;

import java.util.UUID;

/**
 * Server-side event that fires when the player is punished.
 */
public interface ServerPunishEvent extends ServerEvent {
	
	/**
	 * Gets the player UUID who's about to be punished.
	 *
	 * @return player {@link UUID}
	 */
	UUID getPlayerUUID();
	
	/**
	 * Gets the {@link ServerPunishment} this player triggered.
	 *
	 * @return {@link ServerPunishment} instance
	 */
	ServerPunishment getPunishments();
}
