package io.github.freshsupasulley.censorcraft.config.punishments;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class Teleport extends ForgePunishment {
	
	@Override
	public void buildConfig()
	{
		define("x_coord", 0D);
		define("y_coord", 50D);
		define("z_coord", 0D);
		define("coords", false, "Teleports to fixed coords instead of relative position");
	}
	
	@Override
	public void punish(ServerPlayer player)
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
	}
}
