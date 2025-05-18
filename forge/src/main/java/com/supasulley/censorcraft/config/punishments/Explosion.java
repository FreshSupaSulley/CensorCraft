package com.supasulley.censorcraft.config.punishments;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

public class Explosion extends PunishmentOption {
	
	private static ConfigValue<Float> EXPLOSION_RADIUS;
	private static ConfigValue<Boolean> EXPLOSION_FIRE, EXPLOSION_GRIEFING;
	
	@Override
	public void build(Builder builder)
	{
		EXPLOSION_RADIUS = builder.defineInRange("explosion_radius", 5f, 0, Float.MAX_VALUE); // it seems by not defining a range, forge thinks the config file is broken
		EXPLOSION_FIRE = builder.define("create_fires", true);
		EXPLOSION_GRIEFING = builder.comment("Explosions break blocks").define("explosion_griefing", true);
	}
	
	@Override
	public void punish(ServerPlayer player)
	{
		Vec3 pos = player.position();
		player.level().explode(null, player.level().damageSources().generic(), new ExplosionDamageCalculator(), pos.x, pos.y, pos.z, EXPLOSION_RADIUS.get(), EXPLOSION_FIRE.get(), EXPLOSION_GRIEFING.get() ? ExplosionInteraction.BLOCK : ExplosionInteraction.NONE);
	}
}
