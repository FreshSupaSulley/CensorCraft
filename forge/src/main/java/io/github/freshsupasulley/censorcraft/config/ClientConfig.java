package io.github.freshsupasulley.censorcraft.config;

import java.util.List;

import io.github.freshsupasulley.LibraryLoader;
import io.github.freshsupasulley.censorcraft.config.ConfigWrapper.ConfigValue;
import net.minecraftforge.fml.config.ModConfig;

public class ClientConfig extends ConfigFile {
	
	public static final int MIN_LATENCY = 100, MAX_LATENCY = 5000;
	
	public static ConfigValue<Boolean> SHOW_TRANSCRIPTION, DEBUG, USE_VULKAN;
	public static ConfigValue<Integer> LATENCY;
	
	public static ConfigValue<String> UNWANTED;
	
	public ClientConfig()
	{
		super(ModConfig.Type.CLIENT);
	}
	
	@Override
	protected void register(ConfigWrapper config)
	{
		ConfigWrapper top = config.sub("top");
		top.builder("key", "the value").build();
		
		top.buildTable("tableee", table ->
		{
			UNWANTED = table.builder("unwanted_key", "abcdefg").addComment("LOVE YOU!!").build();
		});
		
//		ConfigWrapper table = top.sub("tableee");
//		UNWANTED = table.builder("unwanted_key", "abcdefg").addComment("LOVE YOU!!").build();
//		ConfigValue<List<ConfigWrapper>> tablerr = top.builder("tableee", List.of(table)).build();
//		
		System.out.println(UNWANTED);
		// List<ConfigWrapper> table = List.of(under);
		// test table array
		// balls.builder("array", List.of(under));
		
		ConfigWrapper balls2 = config.sub("balls2");
		balls2.builder("key", "the value").build();
		balls2.builder("number", 1234).build();
		
		SHOW_TRANSCRIPTION = config.builder("general.show_transcription", false).build();
		DEBUG = config.builder("general.debug", false).build();
		LATENCY = config.builder("general.latency", 1000).setRange(MIN_LATENCY, MAX_LATENCY).build();// .addValidator(t -> t > MIN_LATENCY && t < MAX_LATENCY));
		USE_VULKAN = config.builder("general.use_vulkan", LibraryLoader.canUseVulkan()).build();
		// UNWANTED = config.builder("hi", "aojasodij").build();
		// ConfigValue<List<Config>> hi = builder("builder.hi", List.of(config, config2)).build();
	}
}
