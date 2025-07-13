package io.github.freshsupasulley.censorcraft.config.punishments;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraftforge.registries.ForgeRegistries;

public class Entities extends ForgePunishment<Entities> {
	
	@Override
	public void build()
	{
		define("entities", new ArrayList<>(List.of("warden", "skeleton")), "Entities to spawn on the player", "Allowed list (case-insensitive, duplicates allowed): " + ForgeRegistries.ENTITY_TYPES.getKeys().stream().map(ResourceLocation::getPath).sorted().collect(Collectors.joining(", ")));
		defineInRange("quantity", 1, 1, Integer.MAX_VALUE, "Number of times the entire list will be spawned");
	}
	
	@Override
	public void punish(ServerPlayer player)
	{
		for(int i = 0; i < config.getInt("quantity"); i++)
		{
			List<String> entities = config.get("entities");
			entities.forEach(element -> ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.withDefaultNamespace(element)).spawn(player.level(), player.blockPosition(), EntitySpawnReason.COMMAND));
		}
	}
	
	@Override
	public Entities newInstance()
	{
		return new Entities();
	}
}
