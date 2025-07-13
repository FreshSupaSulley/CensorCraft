package io.github.freshsupasulley.plugins.impl.client;

import io.github.freshsupasulley.censorcraft.api.events.client.ClientPunishedEvent;
import io.github.freshsupasulley.plugins.impl.EventImpl;

public class ClientPunishedEventImpl extends EventImpl implements ClientPunishedEvent {
	
	private String[] punishments;
	
	public ClientPunishedEventImpl(String... punishments)
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
