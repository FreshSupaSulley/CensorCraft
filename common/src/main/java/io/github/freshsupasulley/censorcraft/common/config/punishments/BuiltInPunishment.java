package io.github.freshsupasulley.censorcraft.common.config.punishments;

import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;
import net.minecraft.server.level.ServerPlayer;

public abstract class BuiltInPunishment extends Punishment {
	
	@Override
	public final void punish(de.maxhenkel.voicechat.api.ServerPlayer player)
	{
		punish((ServerPlayer) player.getPlayer());
	}
	
	abstract void punish(ServerPlayer player);
}
