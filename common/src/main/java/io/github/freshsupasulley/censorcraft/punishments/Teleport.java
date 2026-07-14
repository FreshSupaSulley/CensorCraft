package io.github.freshsupasulley.censorcraft.punishments;

import io.github.freshsupasulley.censorcraft.api.punishments.PunishmentJob;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public class Teleport extends BuiltInPunishment {
	
	@Override
	public String getId()
	{
		return "teleport";
	}
	
	@Override
	public void buildConfig()
	{
		define("x_coord", 0D);
		define("y_coord", 50D);
		define("z_coord", 0D);
		define("coords", false, "Teleports to fixed coords instead of relative position");
	}
	
	@Override
	public PunishmentJob punish(ServerPlayer player, Set<String> taboos)
	{
		Vec3 pos = player.position();
		boolean coords = config.get("coords");
		double x = config.get("x_coord"), y = config.get("y_coord"), z = config.get("z_coord");
		
		if(coords)
		{
			player.teleportTo(x, y, z);
		}
		else
		{
			player.teleportTo(x + pos.x, y + pos.y, z + pos.z);
		}
		
		return null;
	}
}
