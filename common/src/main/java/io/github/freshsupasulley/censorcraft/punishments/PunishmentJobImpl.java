package io.github.freshsupasulley.censorcraft.punishments;

import io.github.freshsupasulley.censorcraft.api.punishments.PunishmentJob;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public abstract class PunishmentJobImpl extends PunishmentJob {
	
	public PunishmentJobImpl(UUID player)
	{
		super(player);
	}
	
	@Override
	public boolean tick(long endTimeNanos, Object player)
	{
		return tick(endTimeNanos, (ServerPlayer) player);
	}
	
	abstract boolean tick(long endTimeNanos, ServerPlayer player);
}
