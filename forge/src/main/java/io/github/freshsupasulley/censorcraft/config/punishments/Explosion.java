package io.github.freshsupasulley.censorcraft.config.punishments;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.phys.Vec3;

public class Explosion extends PunishmentOption<Explosion> {
	
	@Override
	public void build()
	{
		defineInRange("explosion_radius", 5D, 0D, Double.MAX_VALUE); // it seems by not defining a range, forge thinks the config file is broken
		define("create_fires", true);
		define("explosion_griefing", true, "Explosions break blocks");
	}
	
	@Override
	public void executePunishment(ServerPlayer player)
	{
		Number radius = config.get("explosion_radius");
		boolean createFires = config.get("create_fires");
		boolean griefing = config.get("explosion_griefing");
		
		Vec3 pos = player.position();
		player.level().explode(null, player.level().damageSources().generic(), new ExplosionDamageCalculator(), pos.x, pos.y, pos.z, radius.floatValue(), createFires, griefing ? ExplosionInteraction.BLOCK : ExplosionInteraction.NONE);
	}
	
	@Override
	Explosion newInstance()
	{
		return new Explosion();
	}
}
