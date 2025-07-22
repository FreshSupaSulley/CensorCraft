package io.github.freshsupasulley.censorcraft.api.events.client;

/**
 * This event is emitted from the client, when it receives a packet from the server indicating they were punished and should clear their audio buffer. This
 * event is not cancellable.
 * 
 * <p>
 * You may find this event useful to hook into a client-side environment to execute client-side code.
 * </p>
 */
public interface ClientAcknowledgePunish extends ClientEvent {
	
	/**
	 * Gets the array of punishments that this player invoked.
	 * 
	 * @return array of punishment names
	 */
	String[] getPunishments();
}
