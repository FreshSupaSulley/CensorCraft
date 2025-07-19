package io.github.freshsupasulley.plugins.impl.client;

import io.github.freshsupasulley.censorcraft.api.events.client.ClientAcknowledgePunish;

public class ClientAcknowledgePunishImpl extends ClientEventImpl implements ClientAcknowledgePunish {
	
	private String[] punishments;
	
	public ClientAcknowledgePunishImpl(String... punishments)
	{
		this.punishments = punishments;
	}
	
	/**
	 * Gets the array of punishment type names that this player caused.
	 * 
	 * @return array of punishment type names
	 */
	@Override
	public String[] getPunishments()
	{
		return punishments;
	}
}
