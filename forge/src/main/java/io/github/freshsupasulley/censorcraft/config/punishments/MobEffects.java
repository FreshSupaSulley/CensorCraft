package io.github.freshsupasulley.censorcraft.config.punishments;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.registries.ForgeRegistries;

public class MobEffects extends ForgePunishment {
	
	@Override
	public String getName()
	{
		return "mob_effects";
	}
	
	@Override
	public void build()
	{
		define("effects", new ArrayList<>(List.of("")), "Potion effects to apply to the player", "Allowed list (case-insensitive): " + ForgeRegistries.MOB_EFFECTS.getKeys().stream().map(ResourceLocation::getPath).sorted().collect(Collectors.joining(", ")));
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
			ForgeRegistries.MOB_EFFECTS.getHolder(ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.withDefaultNamespace(effect))).ifPresentOrElse(h -> player.addEffect(new MobEffectInstance(h, ticks, amplifier)), () ->
			{
				CensorCraft.LOGGER.warn("Failed to find mob effect with name {}", effect);
			});
		}
	}
}