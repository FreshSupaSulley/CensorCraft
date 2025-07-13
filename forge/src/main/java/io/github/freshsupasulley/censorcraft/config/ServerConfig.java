package io.github.freshsupasulley.censorcraft.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigSpec;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;
import io.github.freshsupasulley.censorcraft.config.punishments.Commands;
import io.github.freshsupasulley.censorcraft.config.punishments.Crash;
import io.github.freshsupasulley.censorcraft.config.punishments.Dimension;
import io.github.freshsupasulley.censorcraft.config.punishments.Entities;
import io.github.freshsupasulley.censorcraft.config.punishments.Explosion;
import io.github.freshsupasulley.censorcraft.config.punishments.Ignite;
import io.github.freshsupasulley.censorcraft.config.punishments.Kill;
import io.github.freshsupasulley.censorcraft.config.punishments.Lightning;
import io.github.freshsupasulley.censorcraft.config.punishments.MobEffects;
import io.github.freshsupasulley.censorcraft.config.punishments.Teleport;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = CensorCraft.MODID)
public class ServerConfig extends ConfigFile {
	
	private static ServerConfig SERVER;
	private static final LevelResource SERVERCONFIG = new LevelResource("serverconfig");
	
	// Static because register will be called before an instance variable is available
	private static List<Punishment<?>> pluginPunishments = new ArrayList<Punishment<?>>();
	
	private Punishment<?>[] allPunishments;
	
	@SubscribeEvent
	private static void serverSetup(ServerAboutToStartEvent event)
	{
		SERVER = new ServerConfig(event.getServer());
	}
	
	public static ServerConfig get()
	{
		if(SERVER == null)
		{
			CensorCraft.LOGGER.error("Tried to access server config before it was initialized");
		}
		
		return SERVER;
	}
	
	/**
	 * Ripped from {@link ServerLifecycleHooks}.
	 * 
	 * @param server MC server
	 * @return path to server config file
	 */
	private static Path getServerConfigPath(final MinecraftServer server)
	{
		final Path serverConfig = server.getWorldPath(SERVERCONFIG);
		
		if(!Files.isDirectory(serverConfig))
		{
			try
			{
				Files.createDirectories(serverConfig);
			} catch(IOException e)
			{
				throw new RuntimeException(e);
			}
		}
		
		return serverConfig;
	}
	
	public ServerConfig(MinecraftServer server)
	{
		super(getServerConfigPath(server), ModConfig.Type.SERVER);
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
	
	public List<Punishment<?>> getPunishments()
	{
		List<Punishment<?>> options = new ArrayList<Punishment<?>>();
		
		// By fucking LAW each default option needs to be in the server config file
		// ^ why did i write this comment in such a demanding manner
		// ^ ah i remember. because we're checking each punishment anyways and the data for at least one better be in there
		for(Punishment<?> punishment : allPunishments)
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
		define("global_taboos", new ArrayList<>(List.of("boom")), "List of forbidden words and phrases (case-insensitive)", "All enabled punishments will fire when they are spoken");
		define("preferred_model", "base.en", "Name of the transcription model players need to use (determines the language and accuracy)", "Better models have larger file sizes. Clients have tiny.en built-in. See https://github.com/ggml-org/whisper.cpp/blob/master/models/README.md#available-models for available models");
		defineInRange("context_length", 3D, 0D, Double.MAX_VALUE, "Maximum amount of time (in seconds) an individual audio recording is. The higher the value, the more intensive on players PCs");
		defineInRange("punishment_cooldown", 0D, 0D, Double.MAX_VALUE, "Delay (in seconds) before a player can be punished again");
		define("chat_taboos", true, "When someone is punished, send what the player said to chat");
		define("isolate_words", true, "If true, only whole words are considered (surrounded by spaces or word boundaries). If false, partial matches are allowed (e.g., 'art' triggers punishment for 'start')");
		define("monitor_voice", true, "Punish players for speaking taboos into their mic");
		define("monitor_chat", true, "Punish players for sending taboos to chat");
		
		// Punishments are special. They are an array of tables
		// List<CommentedConfig> punishments = new ArrayList<>();
		
		Punishment<?>[] defaults = new Punishment[] {new Commands(), new Crash(), new Dimension(), new Entities(), new Explosion(), new Ignite(), new Kill(), new Lightning(), new MobEffects(), new Teleport()};
		
		System.out.println(pluginPunishments);
		// Add the default punishments below the plugin-defined ones ig
		Stream.of(defaults).forEach(punishment -> pluginPunishments.add(punishment));
		
		// Set all punishments
		this.allPunishments = pluginPunishments.toArray(Punishment[]::new);
		
		for(Punishment<?> option : allPunishments)
		{
			try
			{
				CommentedConfig table = config.createSubConfig();
				option.fillConfig(table);
				// punishments.add(table);
				// Intentionally lax validator, otherwise NightConfig freaks out
				spec.define(option.getName(), List.of(table), value -> value instanceof List);
				// Very sadly, we can't add comments to array of tables in night config :(
				// might need to switch to new library one day
				// config.setComment(option.getName(), option.getDescription());
			} catch(Exception e)
			{
				CensorCraft.LOGGER.warn("An error occured defining the config for punishment type '{}'", option.getName(), e);
			}
		}
		
		// spec.define("punishments", punishments);
	}
	
	public <T extends Punishment<T>> void registerPunishment(Punishment<T> punishment)
	{
		pluginPunishments.add(punishment);
	}
}
