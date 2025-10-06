package io.github.freshsupasulley.censorcraft.punishments;

import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;
import net.minecraft.server.level.ServerPlayer;

public abstract class BuiltInPunishment extends Punishment {
	
	@Override
	public final void punish(Object player)
	{
		punish((ServerPlayer) player);
	}
	
	abstract void punish(ServerPlayer player);
}
