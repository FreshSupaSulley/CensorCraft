package io.github.freshsupasulley.censorcraft.config;

import io.github.freshsupasulley.LibraryLoader;
import io.github.freshsupasulley.censorcraft.config.ConfigWrapper.ConfigValue;
import net.minecraftforge.fml.config.ModConfig;

public class ClientConfig extends ConfigFile {
	
	public static final int MIN_LATENCY = 100, MAX_LATENCY = 5000;
	
	public static ConfigValue<Boolean> SHOW_TRANSCRIPTION, DEBUG, USE_VULKAN;
	public static ConfigValue<Integer> LATENCY;
	
	public ClientConfig()
	{
		super(ModConfig.Type.CLIENT);
	}
	
	@Override
	protected void register(ConfigWrapper config)
	{
		ConfigWrapper top = config.sub("top");
		top.define("key", "the value").build();
		
//		Config hi = Config.inMemory();
//		hi.set("balls", "oner");
//		UNWANTED = config.builder("test", List.of(hi)).build();
		
//		ConfigWrapper table = config.createSubConfig();
//		UNWANTED = table.builder("unwanted_key", "abcdefg").addComment("LOVE YOU!!").build();
//		ConfigValue<List<ConfigWrapper>> tablerr = top.builder("tableee", List.of(table)).build();
		
		// List<ConfigWrapper> table = List.of(under);
		// test table array
		// balls.builder("array", List.of(under));
		
		ConfigWrapper balls2 = config.sub("balls2");
		balls2.define("key", "the value").build();
		balls2.define("number", 1234).build();
		
		SHOW_TRANSCRIPTION = config.define("general.show_transcription", false).build();
		DEBUG = config.define("general.debug", false).build();
		LATENCY = config.define("general.latency", 1000).setRange(MIN_LATENCY, MAX_LATENCY).build();// .addValidator(t -> t > MIN_LATENCY && t < MAX_LATENCY));
		USE_VULKAN = config.define("general.use_vulkan", LibraryLoader.canUseVulkan()).build();
		// UNWANTED = config.builder("hi", "aojasodij").build();
		// ConfigValue<List<Config>> hi = builder("builder.hi", List.of(config, config2)).build();
	}
}
