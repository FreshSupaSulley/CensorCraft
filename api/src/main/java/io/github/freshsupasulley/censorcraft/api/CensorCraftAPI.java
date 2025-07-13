package io.github.freshsupasulley.censorcraft.api;

import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;

/**
 * Handles basic interaction with CensorCraft.
 */
public interface CensorCraftAPI {
	
	/**
	 * Registers a new punishment type that will be added to the server config file for the server admin to manage.
	 * 
	 * @param <T>        punishment type (self-referential generic type)
	 * @param punishment the punishment to add
	 */
	<T extends Punishment<T>> void registerPunishment(Punishment<T> punishment);
}
