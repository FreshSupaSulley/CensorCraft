package io.github.freshsupasulley.censorcraft.punishments;

import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;
import io.github.freshsupasulley.censorcraft.api.punishments.PunishmentJob;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

public abstract class BuiltInPunishment extends Punishment {
	
	@Override
	public final PunishmentJob punish(Object player, Set<String> taboos)
	{
		return punish((ServerPlayer) player, taboos);
	}
	
	abstract PunishmentJob punish(ServerPlayer player, Set<String> taboos);
}
