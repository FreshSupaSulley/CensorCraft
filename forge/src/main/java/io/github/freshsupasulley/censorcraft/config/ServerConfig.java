package io.github.freshsupasulley.censorcraft.config;

import java.util.ArrayList;
import java.util.List;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigSpec;

import io.github.freshsupasulley.censorcraft.config.punishments.Commands;
import io.github.freshsupasulley.censorcraft.config.punishments.PunishmentOption;
import net.minecraftforge.fml.config.ModConfig;

public class ServerConfig extends ConfigFile {
	
	private PunishmentOption<?>[] defaults;
	
	public ServerConfig()
	{
		super(ModConfig.Type.SERVER);
	}
	
	public List<String> getGlobalTaboos()
	{
		return config.get("global_taboos");
	}
	
	public String getPreferredModel()
	{
		return config.get("preferred_model");
	}
	
	public double getContextLength()
	{
		return config.get("context_length");
	}
	
	public double getPunishmentCooldown()
	{
		return config.get("punishment_cooldown");
	}
	
	public boolean isChatTaboos()
	{
		return config.get("chat_taboos");
	}
	
	public boolean isExposeRats()
	{
		return config.get("expose_rats");
	}
	
	public boolean isIsolateWords()
	{
		return config.get("isolate_words");
	}
	
	public boolean isMonitorVoice()
	{
		return config.get("monitor_voice");
	}
	
	public boolean isMonitorChat()
	{
		return config.get("monitor_chat");
	}
	
	public List<PunishmentOption<?>> getPunishments()
	{
		List<PunishmentOption<?>> options = new ArrayList<PunishmentOption<?>>();
		
		// By fucking LAW each default option needs to be in the server config file
		for(PunishmentOption<?> punishment : defaults)
		{
			// array of tables not attack on titan :(
			List<CommentedConfig> aot = config.get(punishment.getName());
			
			aot.forEach(config ->
			{
				options.add(punishment.deserialize(config));
			});
		}
		
		return options;
	}
	
	@Override
	void register(ConfigSpec spec)
	{
		spec.define("global_taboos", new ArrayList<>(List.of("boom")));
		spec.define("preferred_model", "base.en");
		defineInRange("context_length", "How many seconds of audio context to retain", 3D, 0D, Double.MAX_VALUE);
		defineInRange("punishment_cooldown", "Delay (in seconds) before a player can be punished again", 0D, 0D, Double.MAX_VALUE);
		spec.define("chat_taboos", true);
		spec.define("expose_rats", true);
		spec.define("isolate_words", true);
		spec.define("monitor_voice", true);
		spec.define("monitor_chat", true);
		
		// Punishments are special. They are an array of tables
		// List<CommentedConfig> punishments = new ArrayList<>();
		defaults = new PunishmentOption[] {new Commands()};
		
		for(PunishmentOption<?> option : defaults)
		{
			CommentedConfig table = config.createSubConfig();
			option.init(false, table);
			// punishments.add(table);
			// Intentionally lax validator, otherwise NightConfig freaks out
			spec.define(option.getName(), List.of(table), value -> value instanceof List);
		}
		
		// spec.define("punishments", punishments);
	}
	
	@Override
	void postLoad()
	{
		addComment("global_taboos", "Words that are always censored regardless of mode");
		addComment("preferred_model", "Name of the preferred model for recognition");
		addComment("chat_taboos", "Whether to censor typed chat");
		addComment("expose_rats", "Enable rat reporting for taboo snitches");
		addComment("isolate_words", "Whether taboo word matching must be isolated (e.g., 'kill' won't match 'skill')");
		addComment("monitor_voice", "Whether to listen to microphone input");
		addComment("monitor_chat", "Whether to monitor typed chat");
		addComment("punishments", "List of enabled punishment options. Each entry is a table with its own settings.");
	}
}
