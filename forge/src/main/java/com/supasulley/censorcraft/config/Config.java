package com.supasulley.censorcraft.config;

import java.util.List;

import com.supasulley.censorcraft.CensorCraft;
import com.supasulley.censorcraft.config.punishments.Commands;
import com.supasulley.censorcraft.config.punishments.Crash;
import com.supasulley.censorcraft.config.punishments.Dimension;
import com.supasulley.censorcraft.config.punishments.Entities;
import com.supasulley.censorcraft.config.punishments.Explosion;
import com.supasulley.censorcraft.config.punishments.Ignite;
import com.supasulley.censorcraft.config.punishments.Kill;
import com.supasulley.censorcraft.config.punishments.Lightning;
import com.supasulley.censorcraft.config.punishments.PotionEffects;
import com.supasulley.censorcraft.config.punishments.PunishmentOption;
import com.supasulley.censorcraft.config.punishments.Sky;

import io.github.freshsupasulley.LibraryLoader;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod.EventBusSubscriber(modid = CensorCraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public abstract class Config {
	
	public static void register(FMLJavaModLoadingContext context)
	{
		context.registerConfig(ModConfig.Type.CLIENT, new Client().register());
		context.registerConfig(ModConfig.Type.SERVER, new Server().register());
	}
	
	protected abstract ForgeConfigSpec register();
	
	public static class Client extends Config {
		
		public static ConfigValue<Boolean> SHOW_TRANSCRIPTION, SHOW_VOLUME_BAR, SHOW_DELAY, SHOW_VAD, VAD, DENOISE, USE_VULKAN;
		public static ConfigValue<Integer> LATENCY;
		public static ConfigValue<String> PREFERRED_MIC;
		
		@Override
		public ForgeConfigSpec register()
		{
			ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
			builder.push("general");
			PREFERRED_MIC = builder.comment("Name of microphone, otherwise uses first available").define("preferred_mic", "");
			// INDICATE_RECORDING = builder.comment("Shows permanent text in-game indicating recording status").define("indicate_recording", true);
			SHOW_TRANSCRIPTION = builder.comment("Display live transcription").define("show_transcription", true);
			SHOW_VOLUME_BAR = builder.comment("Display microphone volume").define("show_mic_volume", false);
			SHOW_DELAY = builder.comment("Display how far behind transcription is").define("show_delay", false);
			SHOW_VAD = builder.comment("Shows when you are speaking. VAD must be true").define("show_vad", false);
			LATENCY = builder.comment("Transcription speed (in milliseconds)").defineInRange("latency", 750, 30, Integer.MAX_VALUE);
			USE_VULKAN = builder.comment("Uses Vulkan-built libraries for Windows GPU support. Can break on some machines").define("use_vulkan", LibraryLoader.canUseVulkan());
			
			// Recording
			builder.pop().comment("Only mess with these settings if you know what you're doing").push("experimental");
			VAD = builder.comment("Voice activity detection").define("vad", true);
			DENOISE = builder.comment("Denoise. Helps with VAD").define("denoise", true);
			
			return builder.build();
		}
	}
	
	public static class Server extends Config {
		
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
			
			GLOBAL_TABOO = builder.comment("List of forbidden words and phrases (case-insensitive)").comment("All enabled punishments will fire when they are spoken").defineListAllowEmpty("taboo", List.of("boom", "fart poop"), element -> true);
			PREFERRED_MODEL = builder.comment("Name of the transcription model players need to use (determines the language and accuracy)").comment("Better models have larger file sizes. Clients have tiny.en built-in. See https://github.com/ggml-org/whisper.cpp/blob/master/models/README.md#available-models for available models").define("preferred_model", "tiny.en");
			CONTEXT_LENGTH = builder.comment("Minimum amount of time (in seconds) it takes to say a forbidden word or phrase. The higher the value, the more intensive on players PCs").defineInRange("context_length", 3f, 0.5f, 60);
			// ENFORCE_MODEL = builder.comment("Requires players download the preferred model").define("enforce_model", false);
			
			MONITOR_CHAT = builder.comment("Punish players for sending taboos to chat").define("monitor_chat", true);
			ISOLATE_WORDS = builder.comment("If true, only whole words are considered (surrounded by spaces or word boundaries). If false, partial matches are allowed (e.g., 'art' triggers punishment for 'start')").define("isolate_words", true);
			PUNISHMENT_COOLDOWN = builder.comment("Delay (in seconds) before a player can be punished again").defineInRange("punishment_cooldown", 0f, 0f, Float.MAX_VALUE);
			EXPOSE_RATS = builder.comment("Rats on players in the chat if no audio data is being received, or they aren't using the appropriate model").define("expose_rats", true);
			RAT_DELAY = builder.comment("Seconds between ratting on players (expose_rats must be true)").defineInRange("rat_delay", 60f, 1, Float.MAX_VALUE);
			CHAT_TABOOS = builder.comment("When someone is punished, send what the player said to chat").define("chat_taboos", true);
			
			builder.push("punishments").comment("List of all punishment options. To enable one, set enabled = true").comment("Each punishment may have their own additional list of taboos that will only trigger that punishment");
			PUNISHMENTS = new PunishmentOption[] {new Commands(), new Crash(), new Dimension(), new Entities(), new Explosion(), new Ignite(), new Kill(), new Lightning(), new PotionEffects(), new Sky()};
			
			for(PunishmentOption option : PUNISHMENTS)
			{
				option.init(builder);
			}
			
			builder.pop();
			
			return builder.build();
		}
	}
}
