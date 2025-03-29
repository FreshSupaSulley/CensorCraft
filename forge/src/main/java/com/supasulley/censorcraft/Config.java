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
	
//	@SubscribeEvent
//	public static void onLoad(final ModConfigEvent event)
//	{
//		switch(event.getConfig().getType())
//		{
//			case CLIENT:
//				client.list.forEach(ConfigResolver::apply);
//				break;
//			case SERVER:
//				server.list.forEach(ConfigResolver::apply);
//				break;
//			default:
//				break;
//		}
//	}
	
	protected abstract ForgeConfigSpec register();
	
	public static class Client extends Config {
		
		public static ConfigValue<Boolean> SHOW_TRANSCRIPTION;
		public static ConfigValue<Long> SAMPLE_LENGTH, SAMPLE_OVERLAP_LENGTH;
		public static ConfigValue<String> PREFERRED_MIC;
		
		@Override
		public ForgeConfigSpec register()
		{
			ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
			builder.push("general");
			PREFERRED_MIC = builder.comment("Name of microphone, otherwise uses first available").define("preferred_mic", "");
//			INDICATE_RECORDING = builder.comment("Shows permanent text in-game indicating recording status").define("indicate_recording", true);
			SHOW_TRANSCRIPTION = builder.comment("Display live transcription").define("show_transcription", true);
			
			// Recording
			builder.pop().comment("Only mess with these settings if you know what you're doing").push("transcription");
			SAMPLE_LENGTH = builder.comment("Audio sample length in milliseconds").defineInRange("sample_length", 2000L, 2000, 10000);
			SAMPLE_OVERLAP_LENGTH = builder.comment("Number of milliseconds audio samples overlap each other").defineInRange("overlap_length", 500L, 0, 500);
			
			return builder.build();
		}
	}
	
	public static class Server extends Config {
		
		public static ConfigValue<List<? extends String>> TABOO;
		public static ConfigValue<Float> EXPLOSION_RADIUS;
		public static ConfigValue<Boolean> EXPOSE_PLAYERS, EXPLOSION_FIRE, EXPLOSION_GRIEFING;
		
		@Override
		public ForgeConfigSpec register()
		{
			ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
			TABOO = builder.comment("List of all forbidden words (case-insensitive)").defineListAllowEmpty("taboo", List.of("boom"), element -> true);
			EXPOSE_PLAYERS = builder.comment("Rats on players in the chat if no audio data is being received", "This does not prevent all forms of fooling CensorCraft").define("expose_rats", true);
			
			// Punishment
			builder.comment("Shit pertaining to shit").push("punishment");
			EXPLOSION_RADIUS = builder.comment("Explosion radius").define("explosion_radius", 5f);
			EXPLOSION_FIRE = builder.comment("Explosions create fires").define("explosion_fire", true);
			EXPLOSION_GRIEFING = builder.comment("Explosions break blocks").define("explosion_griefing", true);
			
			return builder.build();
		}
	}
}
