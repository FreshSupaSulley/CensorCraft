package io.github.freshsupasulley.censorcraft.api.events.server;

import java.util.UUID;

/**
 * Fires when the server receives transcription results from a player and before analyzing if the player should be
 * punished.
 *
 * <p>Like {@link ServerPunishEvent}, cancelling this will prevent any punishments from occurring including announcing
 * the punishment to chat (if enabled).</p>
 */
public interface ReceiveTranscription extends ServerEvent {
	
	/**
	 * Gets the player UUID responsible for triggering this event.
	 *
	 * @return player {@link UUID}
	 */
	UUID getPlayerUUID();
	
	/**
	 * Gets the text sent to the server from the player.
	 *
	 * @return text sent from the player to the server
	 */
	String getText();
}
