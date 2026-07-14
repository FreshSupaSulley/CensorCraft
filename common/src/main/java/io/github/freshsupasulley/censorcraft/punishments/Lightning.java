package io.github.freshsupasulley.censorcraft.punishments;

import io.github.freshsupasulley.censorcraft.api.punishments.PunishmentJob;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public class Lightning extends BuiltInPunishment {
	
	@Override
	public String getId()
	{
		return "lightning";
	}
	
	@Override
	public void buildConfig()
	{
		defineInRange("strikes", 1, 1, 1000, "Number of lightning bolts", "Successive lightning bolts doesn't seem to increase damage proportionately");
	}
	
	@Override
	public PunishmentJob punish(ServerPlayer player, Set<String> taboos)
	{
		Vec3 pos = player.position();
		
		// Number of strikes
		for(int i = 0; i < config.getInt("strikes"); i++)
		{
			LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, player.level());
			bolt.setPos(pos);
			player.level().addFreshEntity(bolt);
		}
		
		return null;
	}
}
