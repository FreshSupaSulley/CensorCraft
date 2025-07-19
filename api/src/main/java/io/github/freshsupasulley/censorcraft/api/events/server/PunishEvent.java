package io.github.freshsupasulley.censorcraft.api.events.server;

import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;

/**
 * Server side event that fires when the player is punished.
 */
public interface PunishEvent extends ServerEvent {
	
	Punishment getPunishments();
}
