package io.github.freshsupasulley.censorcraft.config.punishments;

import java.util.List;
import java.util.stream.Collectors;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.registries.ForgeRegistries;

public class MobEffects extends PunishmentOption {
	
	private static ConfigValue<List<? extends String>> EFFECTS;
	private static ConfigValue<Integer> DURATION;
	private static ConfigValue<Integer> AMPLIFIER;
	
	@Override
	public String getName()
	{
		return "mob_effects";
	}
	
	@Override
	public void build(Builder builder)
	{
		EFFECTS = builder.comment("Potion effects to apply to the player").comment("Allowed list (case-insensitive): " + ForgeRegistries.MOB_EFFECTS.getKeys().stream().map(ResourceLocation::getPath).sorted().collect(Collectors.joining(", "))).defineList("effects", List.of(), element -> ForgeRegistries.MOB_EFFECTS.getKeys().stream().map(ResourceLocation::getPath).anyMatch(element.toString()::equalsIgnoreCase));
		DURATION = builder.comment("Number of game ticks effects are active").define("duration", 10);
		AMPLIFIER = builder.defineInRange("amplifier", 1, 0, 255);
	}
	
	@Override
	public void punish(ServerPlayer player)
	{
		for(String effect : EFFECTS.get())
		{
			ForgeRegistries.MOB_EFFECTS.getHolder(ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.withDefaultNamespace(effect))).ifPresentOrElse(h -> player.addEffect(new MobEffectInstance(h, DURATION.get(), AMPLIFIER.get())), () ->
			{
				CensorCraft.LOGGER.warn("Failed to find mob effect with name {}", effect);
			});
		}
	}
}