package io.github.freshsupasulley.censorcraft.api.punishments;

/**
 * A punishment that will be fired on the server-side.
 */
public abstract class ServerPunishment extends Punishment {
	
	/**
	 * Punishes the player for this punishment type.
	 *
	 * @param serverPlayer the <code>net.minecraft.server.level</code> instance (you'll need to cast it yourself)
	 */
	public abstract void punish(Object serverPlayer);
}
