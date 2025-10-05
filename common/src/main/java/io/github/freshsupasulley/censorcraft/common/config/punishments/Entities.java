package io.github.freshsupasulley.censorcraft.common.config.punishments;

import io.github.freshsupasulley.censorcraft.common.CensorCraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Entities extends BuiltInPunishment {
	
	@Override
	public String getId()
	{
		return "entities";
	}
	
	@Override
	public void buildConfig()
	{
		define("entities", new ArrayList<>(List.of("warden", "skeleton")), "Entities to spawn on the player", "Allowed list (case-insensitive, duplicates allowed): " + BuiltInRegistries.ENTITY_TYPE.keySet().stream().map(ResourceLocation::getPath).sorted().collect(Collectors.joining(", ")));
		defineInRange("quantity", 1, 1, Integer.MAX_VALUE, "Number of times the entire list will be spawned");
	}
	
	@Override
	public void punish(ServerPlayer player)
	{
		for(int i = 0; i < config.getInt("quantity"); i++)
		{
			List<String> entities = config.get("entities");
			
			entities.forEach(element -> BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.withDefaultNamespace(element)).ifPresentOrElse(entity -> entity.value().spawn(player.level(), player.blockPosition(), EntitySpawnReason.COMMAND), () ->
			{
				CensorCraft.LOGGER.warn("Failed to find entity type with name {}", element);
			}));
		}
	}
}
