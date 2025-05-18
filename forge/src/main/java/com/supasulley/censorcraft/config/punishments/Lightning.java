package com.supasulley.censorcraft.config.punishments;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

public class Lightning extends PunishmentOption {
	
	private static ConfigValue<Integer> LIGHTNING_STRIKES;
	
	@Override
	public void build(Builder builder)
	{
		LIGHTNING_STRIKES = builder.comment("Number of lightning bolts").comment("Successive lightning bolts doesn't seem to increase damage proportionately").defineInRange("strikes", 1, 1, 1000);
	}
	
	@Override
	public void punish(ServerPlayer player)
	{
		Vec3 pos = player.position();
		
		// Number of strikes
		for(int i = 0; i < LIGHTNING_STRIKES.get(); i++)
		{
			LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, player.level());
			bolt.setPos(pos);
			player.level().addFreshEntity(bolt);
		}
	}
}
