package com.supasulley.censorcraft;

import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = CensorCraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public abstract class Config {
	
	public static void register(FMLJavaModLoadingContext context)
	{
		context.registerConfig(ModConfig.Type.CLIENT, new Client().register());
		context.registerConfig(ModConfig.Type.SERVER, new Server().register());
	}
	
	protected abstract ForgeConfigSpec register();
	
	public static class Client extends Config {
		
		public static ConfigValue<Boolean> SHOW_TRANSCRIPTION, SHOW_VOLUME_BAR, SHOW_DELAY, SHOW_VAD, VAD, DENOISE;
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
			
			// Recording
			builder.pop().comment("Only mess with these settings if you know what you're doing").push("advanced");
			VAD = builder.comment("Voice activity detection").define("vad", true);
			DENOISE = builder.comment("Denoise. Helps with VAD").define("denoise", true);
			
			return builder.build();
		}
	}
	
	public static class Server extends Config {
		
		// General
		public static ConfigValue<List<? extends String>> TABOO;
		public static ConfigValue<String> PREFERRED_MODEL;
		public static ConfigValue<Float> PUNISHMENT_COOLDOWN, RAT_DELAY;
		public static ConfigValue<Boolean> CHAT_TABOOS, KILL_PLAYER, IGNORE_TOTEMS, ISOLATE_WORDS, MONITOR_CHAT;
		
		// Explosion
		public static ConfigValue<Float> EXPLOSION_RADIUS;
		public static ConfigValue<Boolean> ENABLE_EXPLOSION, EXPOSE_RATS, EXPLOSION_FIRE, EXPLOSION_GRIEFING;
		
		// Lightning
		public static ConfigValue<Boolean> ENABLE_LIGHTNING;
		public static ConfigValue<Integer> LIGHTNING_STRIKES;
		
		// Sky
		public static ConfigValue<Boolean> ENABLE_SKY, TELEPORT;
		public static ConfigValue<Float> LAUNCH_HEIGHT;
		
		// Ignite
		public static ConfigValue<Boolean> ENABLE_IGNITE;
		public static ConfigValue<Float> IGNITE_SECONDS;
		
		// Anvil
		public static ConfigValue<Boolean> ENABLE_ANVIL;
		public static ConfigValue<Integer> ANVIL_HEIGHT, ANVIL_RADIUS;
		
		// Dimension
		public static ConfigValue<Boolean> ENABLE_DIMENSION, ENABLE_SAFE_TELEPORT, AVOID_NETHER_ROOF, SUMMON_DIRT, ENABLE_FALLBACK;
		public static ConfigValue<Dimension> DIMENSION, FALLBACK_DIMENSION;
		
		// Mob effects
		public static ConfigValue<Boolean> ENABLE_POTION_EFFECTS;
		public static ConfigValue<List<? extends String>> POTION_EFFECTS;
		
		// Entities
		public static ConfigValue<Boolean> ENABLE_ENTITIES;
		public static ConfigValue<List<? extends String>> ENTITIES;
		
		// Commands
		public static ConfigValue<Boolean> ENABLE_COMMANDS;
		public static ConfigValue<List<? extends String>> COMMANDS;
		
		@Override
		public ForgeConfigSpec register()
		{
			ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
			
			TABOO = builder.comment("List of forbidden words and phrases (case-insensitive)").defineListAllowEmpty("taboo", List.of("boom", "fart poop"), element -> true);
			PREFERRED_MODEL = builder.comment("Name of the model players need to use for transcription (determines the language and accuracy). Better models have larger file sizes.").comment("See https://github.com/ggml-org/whisper.cpp/blob/master/models/README.md#available-models").define("preferred_model", "tiny.en");
//			ENFORCE_MODEL = builder.comment("Requires players download the preferred model").define("enforce_model", false);
			
			MONITOR_CHAT = builder.comment("Punishes for sending forbidden words to chat").define("monitor_chat", true);
			ISOLATE_WORDS = builder.comment("If true, only whole words are considered (surrounded by spaces or word boundaries). If false, partial matches are allowed (e.g., 'art' triggers punishment for 'start')").define("isolate_words", true);
			PUNISHMENT_COOLDOWN = builder.comment("Delay (in seconds) before a player can be punished again").defineInRange("punishment_cooldown", 3f, 3f, Float.MAX_VALUE);
			EXPOSE_RATS = builder.comment("Rats on players in the chat if no audio data is being received, or they aren't using the appropriate model").define("expose_rats", true);
			RAT_DELAY = builder.comment("Seconds between ratting on players (expose_rats must be true)").defineInRange("rat_delay", 60f, 1, Float.MAX_VALUE);
			CHAT_TABOOS = builder.comment("Sends what the player said to chat").define("chat_taboos", true);
			
			// Punishment
			builder.push("punishments");
			
			// General settings
			KILL_PLAYER = builder.comment("Guarantees the player dies").define("kill_player", false);
			IGNORE_TOTEMS = builder.comment("Killing the player ignores totems (kill_player must be true)").define("ignore_totem", false);
			
			builder.push("explosion");
			ENABLE_EXPLOSION = builder.define("enable", false);
			EXPLOSION_RADIUS = builder.defineInRange("explosion_radius", 5f, 0, Float.MAX_VALUE); // it seems by not defining a range, forge thinks the config file is broken
			EXPLOSION_FIRE = builder.define("create_fires", true);
			EXPLOSION_GRIEFING = builder.comment("Explosions break blocks").define("explosion_griefing", true);
			builder.pop();
			
			// Lightning
			builder.push("lightning");
			ENABLE_LIGHTNING = builder.define("enable", false);
			LIGHTNING_STRIKES = builder.comment("Number of lightning bolts").comment("Successive lightning bolts doesn't seem to increase damage proportionately").defineInRange("strikes", 1, 1, 1000);
			builder.pop();
			
			// Sky
			builder.comment("Launches the player into the sky").push("sky");
			ENABLE_SKY = builder.define("enable", false);
			LAUNCH_HEIGHT = builder.comment("Launch velocity").defineInRange("launch_velocity", 100f, Float.MIN_VALUE, Float.MAX_VALUE);
			TELEPORT = builder.comment("Teleports the player to the sky instead of launching them").comment("Adds launch_height to the player's Y position").define("teleport", false);
			builder.pop();
			
			// Ignite
			builder.comment("Puts the player on fire").push("ignite");
			ENABLE_IGNITE = builder.define("enable", false);
			IGNITE_SECONDS = builder.comment("Amount of seconds player is on fire for").defineInRange("ignite_seconds", 5f, 0, Float.MAX_VALUE);
			builder.pop();
			
			// Anvil
			builder.comment("Summons an anvil on top of the player").push("anvil");
			ENABLE_ANVIL = builder.define("enable", false);
			ANVIL_HEIGHT = builder.comment("Number of blocks above the player the anvil(s) will spawn").defineInRange("anvil_height", 1, 1, Integer.MAX_VALUE);
			ANVIL_RADIUS = builder.comment("Number of blocks that make up the radius of the anvil circle").defineInRange("anvil_radius", 1, 1, Integer.MAX_VALUE);
			builder.pop();
			
			// Dimension
			builder.comment("Sends the player to a dimension").push("tp_to_dimension");
			ENABLE_DIMENSION = builder.define("enable", false);
			builder.comment("Using RANDOM means the player will be sent to a dimension they are not already in");
			DIMENSION = builder.comment("Dimension to send the player to").defineEnum("dimension", Dimension.NETHER);
			
			builder.comment("Tries to put the player in a safe position").push("safe_teleport");
			ENABLE_SAFE_TELEPORT = builder.define("enable", true);
			AVOID_NETHER_ROOF = builder.comment("Avoids putting the player on the nether roof (not a guarantee)").define("avoid_nether_roof", true);
			SUMMON_DIRT = builder.comment("Places a dirt block below the players feet if they're going to fall (useful in the end)").define("summon_dirt_block", true);
			ENABLE_FALLBACK = builder.comment("Sends the player to another dimension if they are already there").define("enable_fallback", true);
			FALLBACK_DIMENSION = builder.comment("Fallback dimension (enable_fallback must be true)").defineEnum("fallback", Dimension.RANDOM);
			builder.pop();
			
			builder.pop();
			
			// Effects
			builder.push("potion_effects");
			ENABLE_POTION_EFFECTS = builder.define("enable", false);
			POTION_EFFECTS = builder.comment("Potion effects to apply to the player").comment("Allowed list (case-insensitive): " + ForgeRegistries.POTIONS.getKeys().stream().map(ResourceLocation::getPath).sorted().collect(Collectors.joining(", "))).defineList("potion_effects", List.of("long_weakness", "harming"), element -> ForgeRegistries.POTIONS.getKeys().stream().map(ResourceLocation::getPath).anyMatch(element.toString()::equalsIgnoreCase));
			builder.pop();
			
			// Entities
			builder.push("entities");
			ENABLE_ENTITIES = builder.define("enable", false);
			ENTITIES = builder.comment("Entities to spawn on the player").comment("Allowed list (case-insensitive): " + ForgeRegistries.ENTITY_TYPES.getKeys().stream().map(ResourceLocation::getPath).sorted().collect(Collectors.joining(", "))).defineList("entities", List.of("warden", "skeleton"), element -> ForgeRegistries.ENTITY_TYPES.getKeys().stream().map(ResourceLocation::getPath).anyMatch(element.toString()::equalsIgnoreCase));
			builder.pop();
			
			// Commands
			builder.push("commands");
			ENABLE_COMMANDS = builder.define("enable", false);
			COMMANDS = builder.defineListAllowEmpty("commands", List.of("/kill @a"), element -> true);
			builder.pop();
			
			// End punishments
			builder.pop();
			return builder.build();
		}
		
		public enum Dimension
		{
			
			OVERWORLD(Level.OVERWORLD), NETHER(Level.NETHER), END(Level.END), RANDOM(null);
			
			private ResourceKey<Level> level;
			
			Dimension(ResourceKey<Level> level)
			{
				this.level = level;
			}
			
			public ResourceKey<Level> toLevel()
			{
				return level;
			}
		}
	}
}
