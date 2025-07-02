package io.github.freshsupasulley.censorcraft.config.punishments;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;

public class Lightning extends PunishmentOption<Lightning> {
	
	@Override
	public void build()
	{
		defineInRange("strikes", 1, 1, 1000, "Number of lightning bolts", "Successive lightning bolts doesn't seem to increase damage proportionately");
	}
	
	@Override
	public void punish(ServerPlayer player)
	{
		Vec3 pos = player.position();
		
		// Number of strikes
		for(int i = 0; i < config.getInt("strikes"); i++)
		{
			LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, player.level());
			bolt.setPos(pos);
			player.level().addFreshEntity(bolt);
		}
	}
	
	@Override
	Lightning newInstance()
	{
		return new Lightning();
	}
}
