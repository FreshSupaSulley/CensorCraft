package io.github.freshsupasulley.censorcraft.api.events.server;

import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;

import java.util.Set;
import java.util.UUID;

/**
 * Fired when the server is about to notify the chat who said a taboo.
 */
public interface ChatTabooEvent extends ServerEvent {
	
	/**
	 * Gets the {@link java.util.Set} of punishments about to be fired.
	 *
	 * @return {@link Set} of punishments
	 */
	Set<Punishment> getPunishments();
	
	/**
	 * Gets the player UUID who's about to be punished.
	 *
	 * @return player {@link UUID}
	 */
	UUID getPlayer();
	
	/**
	 * Overrides the <code>net.minecraft.network.chat.Component</code> instance about to be sent to chat.
	 *
	 * @param component <code>net.minecraft.network.chat.Component</code> instance
	 */
	void setText(Object component);
	
	/**
	 * Gets the <code>net.minecraft.network.chat.Component</code> object about to be sent.
	 *
	 * @return <code>net.minecraft.network.chat.Component</code> instance
	 */
	Object getText();
}
