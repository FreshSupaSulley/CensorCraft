package com.supasulley.censorcraft.config.punishments;

import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.registries.ForgeRegistries;

public class PotionEffects extends PunishmentOption {
	
	private static ConfigValue<List<? extends String>> POTION_EFFECTS;
	
	@Override
	public String getName()
	{
		return "potion_effects";
	}
	
	@Override
	public void build(Builder builder)
	{
		POTION_EFFECTS = builder.comment("Potion effects to apply to the player").comment("Allowed list (case-insensitive): " + ForgeRegistries.POTIONS.getKeys().stream().map(ResourceLocation::getPath).sorted().collect(Collectors.joining(", "))).defineList("potion_effects", List.of("long_weakness", "harming"), element -> ForgeRegistries.POTIONS.getKeys().stream().map(ResourceLocation::getPath).anyMatch(element.toString()::equalsIgnoreCase));
	}
	
	@Override
	public void punish(ServerPlayer player)
	{
		for(String potion : POTION_EFFECTS.get())
		{
			// ig some potions have multiple effects (im a fake minecraft player)
			ForgeRegistries.POTIONS.getValue(ResourceLocation.withDefaultNamespace(potion)).getEffects().forEach(player::addEffect);
		}
	}
}