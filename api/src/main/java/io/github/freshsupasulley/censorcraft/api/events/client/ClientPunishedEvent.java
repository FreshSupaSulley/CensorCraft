package io.github.freshsupasulley.censorcraft.api.events.client;

/**
 * This event is emitted the client receives a packet from the server indicating they were punished and should clear their audio buffer.
 * 
 * <p>
 * You may find this event useful to hook into a client-side environment to execute client-side code.
 * </p>
 */
public interface ClientPunishedEvent extends ClientEvent {
	
	/**
	 * Gets the list of punishments that this player invoked.
	 * 
	 * @return list of punishment names
	 */
	String[] getPunishments();
}
