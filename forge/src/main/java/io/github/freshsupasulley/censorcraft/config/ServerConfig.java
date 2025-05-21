package io.github.freshsupasulley.censorcraft.config;

import java.util.List;
import java.util.stream.Stream;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import io.github.freshsupasulley.censorcraft.config.punishments.Commands;
import io.github.freshsupasulley.censorcraft.config.punishments.Crash;
import io.github.freshsupasulley.censorcraft.config.punishments.Dimension;
import io.github.freshsupasulley.censorcraft.config.punishments.Entities;
import io.github.freshsupasulley.censorcraft.config.punishments.Explosion;
import io.github.freshsupasulley.censorcraft.config.punishments.Ignite;
import io.github.freshsupasulley.censorcraft.config.punishments.Kill;
import io.github.freshsupasulley.censorcraft.config.punishments.Lightning;
import io.github.freshsupasulley.censorcraft.config.punishments.PotionEffects;
import io.github.freshsupasulley.censorcraft.config.punishments.PunishmentOption;
import io.github.freshsupasulley.censorcraft.config.punishments.Sky;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = CensorCraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ServerConfig extends Config {
	
	private static ForgeConfigSpec spec;
	
	// General
	public static ConfigValue<List<? extends String>> GLOBAL_TABOO;
	public static ConfigValue<String> PREFERRED_MODEL;
	public static ConfigValue<Float> CONTEXT_LENGTH, PUNISHMENT_COOLDOWN, RAT_DELAY;
	public static ConfigValue<Boolean> CHAT_TABOOS, EXPOSE_RATS, ISOLATE_WORDS, MONITOR_CHAT;
	
	public static PunishmentOption[] PUNISHMENTS;
	
	@Override
	public ForgeConfigSpec register()
	{
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		
		GLOBAL_TABOO = builder.comment("List of forbidden words and phrases (case-insensitive)").comment("All enabled punishments will fire when they are spoken").defineListAllowEmpty("taboo", List.of("boom"), element -> true);
		PREFERRED_MODEL = builder.comment("Name of the transcription model players need to use (determines the language and accuracy)").comment("Better models have larger file sizes. Clients have tiny.en built-in. See https://github.com/ggml-org/whisper.cpp/blob/master/models/README.md#available-models for available models").define("preferred_model", "tiny.en");
		CONTEXT_LENGTH = builder.comment("Minimum amount of time (in seconds) it takes to say a forbidden word or phrase. The higher the value, the more intensive on players PCs").defineInRange("context_length", 3f, 0.5f, 60);
		// ENFORCE_MODEL = builder.comment("Requires players download the preferred model").define("enforce_model", false);
		
		MONITOR_CHAT = builder.comment("Punish players for sending taboos to chat").define("monitor_chat", true);
		ISOLATE_WORDS = builder.comment("If true, only whole words are considered (surrounded by spaces or word boundaries). If false, partial matches are allowed (e.g., 'art' triggers punishment for 'start')").define("isolate_words", true);
		PUNISHMENT_COOLDOWN = builder.comment("Delay (in seconds) before a player can be punished again").defineInRange("punishment_cooldown", 0f, 0f, Float.MAX_VALUE);
		EXPOSE_RATS = builder.comment("Rats on players in the chat if no audio data is being received, or they aren't using the appropriate model").define("expose_rats", true);
		RAT_DELAY = builder.comment("Seconds between ratting on players (expose_rats must be true)").defineInRange("rat_delay", 60f, 1, Float.MAX_VALUE);
		CHAT_TABOOS = builder.comment("When someone is punished, send what the player said to chat").define("chat_taboos", true);
		
		// Begin punishments section
		builder.comment("List of all punishment options. To enable one, set enabled = true").comment("Each punishment may have their own additional list of taboos that will only trigger that punishment").push("punishments");
		
		// explosion is enabled by default
		PUNISHMENTS = new PunishmentOption[] {new Commands(), new Crash(), new Dimension(), new Entities(), new Explosion(true), new Ignite(), new Kill(), new Lightning(), new PotionEffects(), new Sky()};
		
		for(PunishmentOption option : PUNISHMENTS)
		{
			option.init(builder);
		}
		
		builder.pop();
		
		return spec = builder.build();
	}
	
	private static void checkConfig(ModConfig config)
	{
		// We're only checking the server config
		if(config.getType() != ModConfig.Type.SERVER || !spec.isLoaded())
			return;
		
		if(!Stream.of(PUNISHMENTS).anyMatch(PunishmentOption::isEnabled))
		{
			CensorCraft.LOGGER.warn("No punishments are enabled. Navigate to {} to enable a punishment", config.getFileName());
		}
	}
	
	@SubscribeEvent
	public static void onConfigLoad(ModConfigEvent.Loading event)
	{
		checkConfig(event.getConfig());
	}
	
	@SubscribeEvent
	public static void onConfigReload(ModConfigEvent.Reloading event)
	{
		checkConfig(event.getConfig());
	}
}