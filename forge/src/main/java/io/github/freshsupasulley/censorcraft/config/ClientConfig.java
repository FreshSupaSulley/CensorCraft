package io.github.freshsupasulley.censorcraft.config;

import java.io.File;

import io.github.freshsupasulley.censorcraft.config.ConfigWrapper.ConfigValue;

public class ClientConfig extends ConfigFile {
	
	public static final int MIN_LATENCY = 100, MAX_LATENCY = 5000;
	
	private boolean SHOW_TRANSCRIPTION, DEBUG, USE_VULKAN;
	private int LATENCY;
	
	public ClientConfig()
	{
		super(new File("client.toml"));
	}
}
