package io.github.freshsupasulley.censorcraft.api.events;

/**
 * This event is emitted the client receives a packet from the server indicating they were punished and should clear their audio buffer.
 * 
 * <p>
 * You may find this event useful to hook into a client-side environment.
 * </p>
 */
public interface ClientReceivePunishEvent extends ClientEvent {
	
	/**
	 * Resets the client's rolling audio buffer.
	 */
	void acknowledge();
}
