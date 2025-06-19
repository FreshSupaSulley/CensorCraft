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
import io.github.freshsupasulley.censorcraft.config.punishments.MobEffects;
import io.github.freshsupasulley.censorcraft.config.punishments.PunishmentOption;
import io.github.freshsupasulley.censorcraft.config.punishments.Teleport;
import io.github.freshsupasulley.censorcraft.network.Trie;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.config.ModConfig.Type;

public class ServerConfig extends Config {
	
	// General
	public static ConfigValue<List<? extends String>> GLOBAL_TABOO;
	public static ConfigValue<String> PREFERRED_MODEL;
	public static ConfigValue<Float> CONTEXT_LENGTH, PUNISHMENT_COOLDOWN;// , RAT_DELAY;
	public static ConfigValue<Boolean> CHAT_TABOOS, /* EXPOSE_RATS, */ISOLATE_WORDS, MONITOR_VOICE, MONITOR_CHAT;
	
	public static PunishmentOption[] PUNISHMENTS;
	
	@Override
	public ForgeConfigSpec register()
	{
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		
		GLOBAL_TABOO = builder.comment("List of forbidden words and phrases (case-insensitive)").comment("All enabled punishments will fire when they are spoken").defineListAllowEmpty("taboo", List.of("boom"), element -> true);
		PREFERRED_MODEL = builder.comment("Name of the transcription model players need to use (determines the language and accuracy)").comment("Better models have larger file sizes. Clients have tiny.en built-in. See https://github.com/ggml-org/whisper.cpp/blob/master/models/README.md#available-models for available models").define("preferred_model", "base.en");
		CONTEXT_LENGTH = builder.comment("Maximum amount of time (in seconds) an individual audio recording is. The higher the value, the more intensive on players PCs").defineInRange("context_length", 5f, 0.5f, 60);
		// ENFORCE_MODEL = builder.comment("Requires players download the preferred model").define("enforce_model", false);
		
		MONITOR_VOICE = builder.comment("Punish players for speaking taboos into their mic").define("monitor_mic", true);
		MONITOR_CHAT = builder.comment("Punish players for sending taboos to chat").define("monitor_chat", true);
		ISOLATE_WORDS = builder.comment("If true, only whole words are considered (surrounded by spaces or word boundaries). If false, partial matches are allowed (e.g., 'art' triggers punishment for 'start')").define("isolate_words", true);
		PUNISHMENT_COOLDOWN = builder.comment("Delay (in seconds) before a player can be punished again").defineInRange("punishment_cooldown", 0f, 0f, Float.MAX_VALUE);
		// EXPOSE_RATS = builder.comment("Rats on players in the chat if no audio data is being received, or they aren't using the appropriate
		// model").define("expose_rats", true);
		// RAT_DELAY = builder.comment("Seconds between ratting on players (expose_rats must be true)").defineInRange("rat_delay", 60f, 1, Float.MAX_VALUE);
		CHAT_TABOOS = builder.comment("When someone is punished, send what the player said to chat").define("chat_taboos", true);
		
		// i don't see a use for this
		// PUNISH_IF_DEAD = builder.comment("Attempts to punish the player even if they're dead or dying").define("punish_if_dead", false);
		
		// Begin punishments section
		builder.comment("List of all punishment options. To enable one, set enabled = true").comment("Each punishment may have their own additional list of taboos that will only trigger that punishment").push("punishments");
		
		// explosion is enabled by default
		PUNISHMENTS = new PunishmentOption[] {new Commands(), new Crash(), new Dimension(), new Entities(), new Explosion(true), new Ignite(), new Kill(), new Lightning(), new MobEffects(), new Teleport()};
		
		for(PunishmentOption option : PUNISHMENTS)
		{
			option.init(builder);
		}
		
		builder.define("hi", new Trie(null));
		
		builder.pop();
		
		return builder.build();
	}
	
	@Override
	protected Type getType()
	{
		return Type.SERVER;
	}
	
	@Override
	protected void onConfigUpdate(ModConfig config)
	{
		if(!Stream.of(PUNISHMENTS).anyMatch(PunishmentOption::isEnabled))
		{
			CensorCraft.LOGGER.warn("No punishments are enabled. Navigate to {} to enable a punishment", config.getFileName());
		}
		
		if(!MONITOR_CHAT.get() && !MONITOR_VOICE.get())
		{
			CensorCraft.LOGGER.warn("You are not monitoring voice or chat! CensorCraft is effectively disabled");
		}
	}
}