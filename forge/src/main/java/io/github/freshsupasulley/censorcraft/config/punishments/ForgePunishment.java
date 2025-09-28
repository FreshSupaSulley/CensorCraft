package io.github.freshsupasulley.censorcraft.config.punishments;

import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;
import net.minecraft.server.level.ServerPlayer;

public abstract class ForgePunishment extends Punishment {
	
	abstract void punish(ServerPlayer player);
	
	@Override
	public void punish(Object player)
	{
		punish((ServerPlayer) player);
	}
}
