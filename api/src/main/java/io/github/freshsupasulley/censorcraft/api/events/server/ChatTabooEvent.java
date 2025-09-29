package io.github.freshsupasulley.censorcraft.api.events.server;

import io.github.freshsupasulley.censorcraft.api.events.PluginPunishments;

import java.util.UUID;

/**
 * Fired when the server is about to notify the chat who said a taboo.
 */
public interface ChatTabooEvent extends ServerEvent {
	
	/**
	 * Gets the {@link PluginPunishments} representing the punishments about to be fired.
	 *
	 * @return {@link PluginPunishments} instance
	 */
	PluginPunishments getPunishments();
	
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
