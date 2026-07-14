package io.github.freshsupasulley.censorcraft.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigSpec;
import io.github.freshsupasulley.censorcraft.CensorCraft;
import io.github.freshsupasulley.censorcraft.api.CensorCraftPlugin;
import io.github.freshsupasulley.censorcraft.api.events.server.ServerConfigEvent;
import io.github.freshsupasulley.censorcraft.api.punishments.IConfigFile;
import io.github.freshsupasulley.censorcraft.api.punishments.PunishmentParam;
import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;
import io.github.freshsupasulley.censorcraft.plugins.impl.server.CensorCraftServerAPIImpl;
import io.github.freshsupasulley.censorcraft.plugins.impl.server.ServerConfigEventImpl;

import java.nio.file.Path;
import java.util.*;

public abstract class ServerConfig extends ConfigFile {
	
	private static ServerConfig INSTANCE;
	
	public ServerConfig(Path configFolder)
	{
		super(configFolder, "server");
		
		INSTANCE = this;
		
		// Now the plugins can see the API impl
		CensorCraftServerAPIImpl.INSTANCE = new CensorCraftServerAPIImpl(INSTANCE.config);
		CensorCraft.events.dispatchEvent(ServerConfigEvent.class, new ServerConfigEventImpl());
	}
	
	public static ServerConfig get()
	{
		if(INSTANCE == null)
		{
			throw new IllegalStateException("Tried to access server config before it was initialized");
		}
		
		return INSTANCE;
	}
	
	/**
	 * Gets a {@link PunishmentParam} value for a particular punishment. If that doesn't exist, we fallback to the
	 * server config value (and then another fallback to the default value).
	 *
	 * @param param      {@link PunishmentParam} to check
	 * @param punishment punishment to check for a global param override
	 * @param <T>        type of value (boolean, double, etc.)
	 * @return value of the setting
	 */
	public <T> T getParam(PunishmentParam<T> param, Punishment punishment)
	{
		var defaultValue = param.getDefaultValue();
		var obj = punishment.getParam(param, param.getValue(config, defaultValue));
		
		return obj;
	}
	
	public boolean isCensorCraftEnabled()
	{
		return config.get("enable_censorcraft");
	}
	
	public String getPreferredModel()
	{
		return config.get("preferred_model");
	}
	
	public double getContextLength()
	{
		return ((Number) config.get("context_length")).doubleValue();
	}
	
	/**
	 * Maps each plugin's ID to an array of deserialized punishments that are currently defined in the server config
	 * file.
	 *
	 * @return map of plugin IDs to a list of {@link Punishment}
	 */
	public List<Punishment> getConfigPunishments()
	{
		List<Punishment> punishmentList = new ArrayList<>();
		
		// For each plugin punishment
		CensorCraft.PLUGIN_PUNISHMENTS.forEach((plugin, classes) ->
		{
			// Now for each punishment defined in this plugin
			classes.forEach(punishment ->
			{
				// array of tables not attack on titan :(
				// By convention, there needs to be at least one of each punishment type in the config file
				List<CommentedConfig> aot = config.get(plugin.getPluginId() + "." + punishment.getId());
				
				// If we have a list
				// ... don't print to logs if null cause that'll spam it
				if(aot != null)
				{
					aot.forEach(config ->
					{
						// ... hence why a Punishment must have a default constructor
						// Punishments are proven to be working at this point, as plugin registration tests instantiation
						Punishment p = Punishment.newInstance(punishment.getClass());
						p.config = config;
						punishmentList.add(p);
					});
				}
			});
		});
		
		return punishmentList;
	}
	
	@Override
	void register(ConfigSpec spec)
	{
		// Super global, non-punishment specific config options
		define("enable_censorcraft", true, "On/off switch for the entire mod", "You can also use the /censorcraft enable/disable commands");
		define("preferred_model", "base.en", "Name of the transcription model players need to use (determines the language and accuracy)", "Better models have larger file sizes. See https://github.com/ggml-org/whisper.cpp/blob/master/models/README.md#available-models for available models");
		defineInRange("context_length", 3D, 0D, Double.MAX_VALUE, "Maximum amount of time (in seconds) an individual audio recording is. The higher the value, the more intensive on players PCs", "Enter a number to at least 1 decimal place!");
		
		// Punishment params that can be applied globally unless explicitly overridden
		for(var param : PunishmentParam.values())
		{
			param.applyDefinitions(this);
		}
		
		// Define (always server) configs for all punishments
		for(Map.Entry<CensorCraftPlugin, Set<Punishment>> plugin : CensorCraft.PLUGIN_PUNISHMENTS.entrySet())
		{
			CensorCraft.LOGGER.info("Defining config for plugin '{}'", plugin.getKey());
			
			// For each punishment defined in this plugin
			for(Punishment option : plugin.getValue())
			{
				CensorCraft.LOGGER.info("Defining config for punishment '{}' of '{}'", option.getId(), plugin.getKey());
				
				try
				{
					// Each punishment creates an array of tables with only one entry
					CommentedConfig table = config.createSubConfig();
					
					// Define default params
					PunishmentParam.values().forEach(v -> v.applyDefinitions(new IConfigFile() {
						
						@Override
						public ConfigSpec getSpec()
						{
							return new ConfigSpec();
						}
						
						@Override
						public Map<String, List<String>> getComments()
						{
							return new HashMap<>();
						}
					}));
					
					option.initialize(table);
					
					// Intentionally lax validator, otherwise NightConfig freaks out
					// We concatenate the plugin and the punishment IDs together with a dot (TOML standard)
					spec.define(plugin.getKey().getPluginId() + "." + option.getId(), List.of(table), value -> value instanceof List);
					// Very sadly, we can't add comments to array of tables in night config :(
					// might need to switch to new library one day
					// config.setComment(option.getName(), option.getDescription());
				} catch(Exception e)
				{
					CensorCraft.LOGGER.warn("An error occurred defining the config for punishment '{}'", option.getId(), e);
				}
			}
		}
	}
}
