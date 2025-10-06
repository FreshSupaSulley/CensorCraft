package io.github.freshsupasulley.censorcraft.punishments;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MobEffects extends BuiltInPunishment {
	
	@Override
	public String getId()
	{
		return "mob_effects";
	}
	
	@Override
	public void buildConfig()
	{
		define("effects", new ArrayList<>(List.of("")), "Potion effects to apply to the player", "Allowed list (case-insensitive): " + BuiltInRegistries.MOB_EFFECT.keySet().stream().map(ResourceLocation::getPath).sorted().collect(Collectors.joining(", ")));
		define("duration", 300, "Number of game ticks effects are active");
		defineInRange("amplifier", 1, 0, 255);
	}
	
	@Override
	public void punish(ServerPlayer player)
	{
		List<String> effects = config.get("effects");
		int ticks = config.getInt("duration");
		int amplifier = config.getInt("amplifier");
		
		for(String effect : effects)
		{
			BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.withDefaultNamespace(effect)).ifPresentOrElse(h -> player.addEffect(new MobEffectInstance(h, ticks, amplifier)), () ->
			{
				CensorCraft.LOGGER.warn("Failed to find mob effect with name {}", effect);
			});
		}
	}
}