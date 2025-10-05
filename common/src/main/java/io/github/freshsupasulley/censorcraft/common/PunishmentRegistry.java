package io.github.freshsupasulley.censorcraft.common;

import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Per-plugin punishment registry.
 */
public class PunishmentRegistry {
	
	private final Map<String, Punishment> REGISTERED = new HashMap<>();
	
	public void register(Punishment punishment)
	{
		if(REGISTERED.containsKey(punishment.getId()))
		{
			throw new IllegalArgumentException("Punishment with ID '" + punishment.getId() + "' was already registered");
		}
		
		REGISTERED.put(punishment.getId(), punishment);
	}
	
//	public Punishment get(String id)
//	{
//		return REGISTERED.get(id);
//	}
	
	public Collection<Punishment> all()
	{
		return REGISTERED.values();
	}
}
