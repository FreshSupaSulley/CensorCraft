package com.supasulley.censorcraft.config.punishments;

import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.registries.ForgeRegistries;

public class Entities extends PunishmentOption {
	
	private static ConfigValue<List<? extends String>> ENTITIES;
	
	@Override
	public String getDescription()
	{
		return "Entities to spawn on the player";
	}
	
	@Override
	public void build(Builder builder)
	{
		ENTITIES = builder.comment("Entities to spawn on the player").comment("Allowed list (case-insensitive): " + ForgeRegistries.ENTITY_TYPES.getKeys().stream().map(ResourceLocation::getPath).sorted().collect(Collectors.joining(", "))).defineList("entities", List.of("warden", "skeleton"), element -> ForgeRegistries.ENTITY_TYPES.getKeys().stream().map(ResourceLocation::getPath).anyMatch(element.toString()::equalsIgnoreCase));
	}
	
	@Override
	public void punish(ServerPlayer player)
	{
		ENTITIES.get().forEach(element -> ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.withDefaultNamespace(element)).spawn(player.serverLevel(), player.blockPosition(), EntitySpawnReason.COMMAND));
	}
}
