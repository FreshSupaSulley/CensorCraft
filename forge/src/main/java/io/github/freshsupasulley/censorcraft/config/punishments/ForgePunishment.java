package io.github.freshsupasulley.censorcraft.config.punishments;

import java.util.UUID;

import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;
import net.minecraft.server.level.ServerPlayer;

public abstract class ForgePunishment<T extends ForgePunishment<T>> extends Punishment<T> {
	
	public abstract void punish(ServerPlayer player);
	
	@Override
	public void punish(UUID player)
	{
		
	}
}
