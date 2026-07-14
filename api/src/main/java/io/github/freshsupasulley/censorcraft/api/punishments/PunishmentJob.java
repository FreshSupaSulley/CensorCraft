package io.github.freshsupasulley.censorcraft.api.punishments;

import java.util.UUID;

/**
 * Use {@link PunishmentJob} to queue intensive server-side punishments that need to be spread out over multiple ticks.
 *
 * <p>The {@link Punishment} should respect the <code>endTimeMs</code> parameter to avoid choking the rest of the
 * game, although it's not enforced.</p>
 */
public abstract class PunishmentJob {
	
	private final long startTimeMs;
	private final UUID playerUUID;
	
	public PunishmentJob(UUID playerUUID)
	{
		this.startTimeMs = System.currentTimeMillis();
		this.playerUUID = playerUUID;
	}
	
	/**
	 * The sacred tick method.
	 *
	 * <p>You are responsible for keeping track of how your punishment breaks logic across ticks. Store an index, a
	 * timer, anything – but once <code>endTimeMs</code> passes, end your method to relinquish control back to the
	 * main thread and prepare for the next tick to call your function again to resume where you left off.</p>
	 *
	 * @param endTimeMs time (in milliseconds) when this function should stop (use {@link System#currentTimeMillis()})
	 * @param player       the <code>net.minecraft.server.level</code> server player object
	 * @return true if the job is complete
	 */
	public abstract boolean tick(long endTimeMs, Object player);
	
	public long getStartTimeMs()
	{
		return startTimeMs;
	}
	
	public UUID getPlayerUUID()
	{
		return playerUUID;
	}
}
