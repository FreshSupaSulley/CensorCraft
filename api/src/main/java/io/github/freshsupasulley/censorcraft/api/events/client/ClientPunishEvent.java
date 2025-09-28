package io.github.freshsupasulley.censorcraft.api.events.client;

import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;

/**
 * Client-side event that fires when the player is told they were punished and is about to execute the punishment on
 * their own machine.
 */
public interface ClientPunishEvent extends ClientEvent {
	
	/**
	 * Gets the punishment this player triggered.
	 *
	 * @return {@link Punishment} object
	 */
	Punishment getPunishment();
}
