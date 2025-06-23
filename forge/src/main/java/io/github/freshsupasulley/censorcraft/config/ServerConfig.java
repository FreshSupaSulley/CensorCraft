package io.github.freshsupasulley.censorcraft.config;

import java.util.List;

import io.github.freshsupasulley.censorcraft.config.ConfigWrapper.ConfigValue;
import io.github.freshsupasulley.censorcraft.config.punishments.Commands;
import io.github.freshsupasulley.censorcraft.config.punishments.PunishmentOption;
import net.minecraftforge.fml.config.ModConfig;

public class ServerConfig extends ConfigFile {
	
	public ServerConfig()
	{
		super(ModConfig.Type.SERVER);
	}
	
	// General
	public static ConfigValue<List<String>> GLOBAL_TABOO;
	public static ConfigValue<String> PREFERRED_MODEL;
	public static ConfigValue<Float> CONTEXT_LENGTH, PUNISHMENT_COOLDOWN;// , RAT_DELAY;
	public static ConfigValue<Boolean> CHAT_TABOOS, /* EXPOSE_RATS, */ISOLATE_WORDS, MONITOR_VOICE, MONITOR_CHAT;
	
	public static PunishmentOption[] PUNISHMENTS;
	
	@Override
	public void register(ConfigWrapper config)
	{
		GLOBAL_TABOO = config.define("taboo", List.of("boom")).comment("List of forbidden words and phrases (case-insensitive)").comment("All enabled punishments will fire when they are spoken").build();
		PREFERRED_MODEL = config.define("preferred_model", "base.en").comment("Name of the transcription model players need to use (determines the language and accuracy)").comment("Better models have larger file sizes. Clients have tiny.en built-in. See https://github.com/ggml-org/whisper.cpp/blob/master/models/README.md#available-models for available models").build();
		CONTEXT_LENGTH = config.define("context_length", 5f).setRange(0.5f, 60f).comment("Maximum amount of time (in seconds) an individual audio recording is. The higher the value, the more intensive on players PCs").build();
		// ENFORCE_MODEL = builder.comment("Requires players download the preferred model").define("enforce_model", false);
		
		MONITOR_VOICE = config.define("monitor_mic", true).comment("Punish players for speaking taboos into their mic").build();
		MONITOR_CHAT = config.define("monitor_chat", true).comment("Punish players for sending taboos to chat").build();
		ISOLATE_WORDS = config.define("isolate_words", true).comment("If true, only whole words are considered (surrounded by spaces or word boundaries). If false, partial matches are allowed (e.g., 'art' triggers punishment for 'start')").build();
		PUNISHMENT_COOLDOWN = config.define("punishment_cooldown", 0f).setRange(0f, Float.MAX_VALUE).comment("Delay (in seconds) before a player can be punished again").build();
		// EXPOSE_RATS = builder.comment("Rats on players in the chat if no audio data is being received, or they aren't using the appropriate
		// model").define("expose_rats", true);
		// RAT_DELAY = builder.comment("Seconds between ratting on players (expose_rats must be true)").defineInRange("rat_delay", 60f, 1, Float.MAX_VALUE);
		CHAT_TABOOS = config.define("chat_taboos", true).comment("When someone is punished, send what the player said to chat").build();
		
		// i don't see a use for this
		// PUNISH_IF_DEAD = builder.comment("Attempts to punish the player even if they're dead or dying").define("punish_if_dead", false);
		
		// Begin punishments section
		ConfigWrapper sub = config.sub("punishments", "List of all punishment options. To enable one, set enabled = true", "Each punishment may have their own additional list of taboos that will only trigger that punishment");
		
		// explosion is enabled by default
		PUNISHMENTS = new PunishmentOption[] {new Commands()};// , new Crash(), new Dimension(), new Entities(), new Explosion(true), new Ignite(), new Kill(), new Lightning(), new MobEffects(), new Teleport()};
		
		for(PunishmentOption option : PUNISHMENTS)
		{
			// Build an array of tables with just one table to start. User can add as many as they want
			sub.buildTable(option.getName(), (table, spec) ->
			{
				option.init(table, spec);
			}, option.getDescription());
			
			// option.init(sub);
		}
	}
	
	// @Override
	// protected void onConfigUpdate(ModConfig config)
	// {
	// if(!Stream.of(PUNISHMENTS).anyMatch(PunishmentOption::isEnabled))
	// {
	// CensorCraft.LOGGER.warn("No punishments are enabled. Navigate to {} to enable a punishment", config.getFileName());
	// }
	//
	// if(!MONITOR_CHAT.get() && !MONITOR_VOICE.get())
	// {
	// CensorCraft.LOGGER.warn("You are not monitoring voice or chat! CensorCraft is effectively disabled");
	// }
	// }
}