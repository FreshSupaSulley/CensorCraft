package com.supasulley.censorcraft;

import java.util.List;

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
		
		public static ConfigValue<Boolean> SHOW_TRANSCRIPTION, SHOW_VOLUME_BAR, SHOW_DELAY;
		public static ConfigValue<Long> SAMPLE_LENGTH, SAMPLE_OVERLAP_LENGTH;
		public static ConfigValue<String> PREFERRED_MIC;
		
		@Override
		public ForgeConfigSpec register()
		{
			ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
			builder.push("general");
			PREFERRED_MIC = builder.comment("Name of microphone, otherwise uses first available").define("preferred_mic", "");
			// INDICATE_RECORDING = builder.comment("Shows permanent text in-game indicating recording status").define("indicate_recording", true);
			SHOW_TRANSCRIPTION = builder.comment("Display live transcription").define("show_transcription", true);
			SHOW_VOLUME_BAR = builder.comment("Display microphone volume").define("show_mic_volume", true);
			SHOW_DELAY = builder.comment("Display how far behind transcription is").define("show_delay", true);
			
			// Recording
			builder.pop().comment("Only mess with these settings if you know what you're doing").push("transcription");
			SAMPLE_LENGTH = builder.comment("Audio sample length in milliseconds").defineInRange("sample_length", 2000L, 2000, 10000);
			SAMPLE_OVERLAP_LENGTH = builder.comment("Number of milliseconds audio samples overlap each other").defineInRange("overlap_length", 500L, 0, 500);
			
			return builder.build();
		}
	}
	
	public static class Server extends Config {
		
		// General
		public static ConfigValue<List<? extends String>> TABOO;
		public static ConfigValue<Float> TABOO_COOLDOWN, RAT_DELAY;
		public static ConfigValue<Boolean> CHAT_TABOOS, KILL_PLAYER, IGNORE_TOTEMS;
		
		// Explosion
		public static ConfigValue<Float> EXPLOSION_RADIUS;
		public static ConfigValue<Boolean> ENABLE_EXPLOSION, EXPOSE_RATS, EXPLOSION_FIRE, EXPLOSION_GRIEFING;
		
		// Lightning
		public static ConfigValue<Boolean> ENABLE_LIGHTNING;
		public static ConfigValue<Integer> LIGHTNING_STRIKES;
		
		@Override
		public ForgeConfigSpec register()
		{
			ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
			TABOO = builder.comment("List of forbidden words (case-insensitive)").defineListAllowEmpty("taboo", List.of("boom"), element -> true);
			TABOO_COOLDOWN = builder.comment("Delay (in seconds) before a player can be punished again").defineInRange("punishment_cooldown", 3f, 3f, Float.MAX_VALUE);
			EXPOSE_RATS = builder.comment("Rats on players in the chat if no audio data is being received").define("expose_rats", true);
			RAT_DELAY = builder.comment("Seconds between ratting on players (expose_rats must be true)").defineInRange("rat_delay", 60f, 10, Float.MAX_VALUE);
			CHAT_TABOOS = builder.comment("Sends what the player said to chat").define("chat_taboos", true);
			KILL_PLAYER = builder.comment("Guarantees the player dies").define("kill_player", true);
			IGNORE_TOTEMS = builder.comment("Killing the player ignores totems (kill_player must be true)").define("ignore_totem", false);
			
			// Punishment
			builder.push("explosion");
			ENABLE_EXPLOSION = builder.define("enable", true);
			EXPLOSION_RADIUS = builder.define("explosion_radius", 5f);
			EXPLOSION_FIRE = builder.define("create_fires", true);
			EXPLOSION_GRIEFING = builder.comment("Explosions break blocks").define("explosion_griefing", true);
			builder.pop();
			
			// Lightning
			builder.push("lightning");
			ENABLE_LIGHTNING = builder.define("enable", false);
			LIGHTNING_STRIKES = builder.comment("Number of lightning bolts").defineInRange("strikes", 1, 1, 1000);
			builder.pop();
			
			return builder.build();
		}
	}
}
