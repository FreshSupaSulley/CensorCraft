package io.github.freshsupasulley.censorcraft.config.punishments;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

public class Teleport extends PunishmentOption {
	
	public static ConfigValue<Boolean> COORDS;
	public static ConfigValue<Double> X, Y, Z;
	
	@Override
	public String getDescription()
	{
		return "Teleports the player relative to their position";
	}
	
	@Override
	public void build(Builder builder)
	{
		X = builder.define("x_coord", 0D);
		Y = builder.define("y_coord", 50D);
		Z = builder.define("z_coord", 0D);
		COORDS = builder.comment("Teleports to fixed coords instead of relative position").define("coords", false);
	}
	
	@Override
	public void punish(ServerPlayer player)
	{
		Vec3 pos = player.position();
		
		if(COORDS.get())
		{
			player.teleportTo(X.get(), Y.get(), Z.get());
		}
		else
		{
			player.teleportTo(X.get() + pos.x, Y.get() + pos.y, Z.get() + pos.z);
		}
	}
}
