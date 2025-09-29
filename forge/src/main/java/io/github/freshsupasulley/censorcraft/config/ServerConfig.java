package io.github.freshsupasulley.censorcraft.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.freshsupasulley.censorcraft.CensorCraft;
import io.github.freshsupasulley.censorcraft.PunishmentRegistry;
import io.github.freshsupasulley.censorcraft.api.events.PluginPunishments;
import io.github.freshsupasulley.censorcraft.api.events.server.ServerConfigEvent;
import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;
import io.github.freshsupasulley.censorcraft.network.PunishedPacket;
import io.github.freshsupasulley.censorcraft.network.WordPacket;
import io.github.freshsupasulley.plugins.impl.CensorCraftServerAPIImpl;
import io.github.freshsupasulley.plugins.impl.server.ServerConfigEventImpl;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = CensorCraft.MODID)
public class ServerConfig extends ConfigFile {
	
	private static ServerConfig SERVER;
	private static final LevelResource SERVERCONFIG = new LevelResource("serverconfig");
	
	@SubscribeEvent
	private static void serverSetup(ServerAboutToStartEvent event)
	{
		SERVER = new ServerConfig(event.getServer());
		// Now the plugins can see the API impl
		CensorCraftServerAPIImpl.INSTANCE = new CensorCraftServerAPIImpl(SERVER.config);
		CensorCraft.events.dispatchEvent(ServerConfigEvent.class, new ServerConfigEventImpl());
	}
	
	@SubscribeEvent
	private static void registerCommands(RegisterCommandsEvent event)
	{
		// probably highest op level for this command? (4)
		event.getDispatcher().register(Commands.literal("censorcraft").requires(source -> source.hasPermission(4)).then(Commands.literal("enable").executes(ctx -> setEnabled(ctx.getSource(), true))).then(Commands.literal("disable").executes(ctx -> setEnabled(ctx.getSource(), false))));
	}
	
	private static int setEnabled(CommandSourceStack source, boolean enabled) throws CommandSyntaxException
	{
		boolean state = get().isCensorCraftEnabled();
		
		if(state == enabled)
		{
			throw new SimpleCommandExceptionType(Component.literal("CensorCraft is already " + (state ? "enabled" : "disabled"))).create();
		}
		
		// If we just enabled it
		if(enabled)
		{
			// Clear the accumulated participants buffer
			WordPacket.resetParticipants();
			
			for(ServerPlayer player : source.getServer().getPlayerList().getPlayers())
			{
				// Signal to clients to reset their audio buffer
				// (so if they spoke a taboo right as its enabled, they don't get punished)
				// This could be paired with temporarily ignoring taboos for like 1s server side if required
				CensorCraft.channel.send(new PunishedPacket(new PluginPunishments(Map.of())), PacketDistributor.PLAYER.with(player));
			}
		}
		
		// Let's also notify them that the mod is active
		source.getLevel().players().forEach(sample -> sample.displayClientMessage(Component.literal("CensorCraft is now ").append(Component.literal(enabled ? "enabled" : "disabled").withStyle(style -> style.withBold(true))), false));
		
		CensorCraft.LOGGER.info("Setting CensorCraft enabled state: {}", enabled);
		get().config.set("enable_censorcraft", enabled);
		return 1;
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
	
	public double getPunishmentCooldown()
	{
		return ((Number) config.get("punishment_cooldown")).doubleValue();
	}
	
	public boolean isChatTaboos()
	{
		return config.get("chat_taboos");
	}
	
	public boolean isIsolateWords()
	{
		return config.get("isolate_words");
	}
	
	public boolean isMonitorChat()
	{
		return config.get("monitor_chat");
	}
	
	/**
	 * Maps each plugin's ID to an array of deserialized punishments that are currently defined in the server config
	 * file.
	 *
	 * @return map of plugin IDs to a list of {@link Punishment}
	 */
	public Map<String, List<Punishment>> getConfigPunishments()
	{
		Map<String, List<Punishment>> options = new HashMap<>();
		
		// For each plugin punishment
		CensorCraft.pluginPunishments.forEach((pluginID, registry) ->
		{
			// Create the punishment list for this plugin
			var punishmentList = new ArrayList<Punishment>();
			
			// Now for each punishment defined in this plugin
			registry.all().forEach(punishment ->
			{
				// array of tables not attack on titan :(
				// By convention, there needs to be at least one of each punishment type in the config file
				List<CommentedConfig> aot = config.get(pluginID + "." + punishment.getId());
				
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
			
			// If we actually put something in the list, then add it to the map
			// This counters the rare case that no punishment was instantiated
			// ... which probably will never happen (read the javadoc of safeInstantiation)
			if(!punishmentList.isEmpty())
			{
				options.put(pluginID, punishmentList);
			}
		});
		
		return options;
	}
	
	@Override
	void register(ConfigSpec spec)
	{
		// Global on / off state
		define("enable_censorcraft", true, "On/off switch for the entire mod", "You can also use the /censorcraft enable/disable commands");
		
		defineList("global_taboos", List.of("boom"), "List of forbidden words and phrases (case-insensitive)", "All enabled punishments will fire when they are spoken");
		define("preferred_model", "base.en", "Name of the transcription model players need to use (determines the language and accuracy)", "Better models have larger file sizes. See https://github.com/ggml-org/whisper.cpp/blob/master/models/README.md#available-models for available models");
		defineInRange("context_length", 3D, 0D, Double.MAX_VALUE, "Maximum amount of time (in seconds) an individual audio recording is. The higher the value, the more intensive on players PCs", "Enter a number to at least 1 decimal place!");
		defineInRange("punishment_cooldown", 0D, 0D, Double.MAX_VALUE, "Delay (in seconds) before a player can be punished again", "Enter a number to at least 1 decimal place!");
		define("chat_taboos", true, "When someone is punished, send what the player said to chat");
		define("isolate_words", true, "If true, only whole words are considered (surrounded by spaces or word boundaries). If false, partial matches are allowed (e.g., 'art' triggers punishment for 'start')");
		define("monitor_chat", true, "Punish players for sending taboos to chat");
		
		// Define (always server) configs for all punishments
		for(Map.Entry<String, PunishmentRegistry> plugin : CensorCraft.pluginPunishments.entrySet())
		{
			CensorCraft.LOGGER.info("Defining config for plugin '{}'", plugin.getKey());
			
			// For each punishment defined in this plugin
			for(Punishment option : plugin.getValue().all())
			{
				CensorCraft.LOGGER.info("Defining config for punishment '{}' of '{}'", option.getId(), plugin.getKey());
				
				try
				{
					// Each punishment creates an array of tables with only one entry
					CommentedConfig table = config.createSubConfig();
					option.buildConfig(table);
					// Intentionally lax validator, otherwise NightConfig freaks out
					// We concatenate the plugin and the punishment IDs together with a dot (TOML standard)
					spec.define(plugin.getKey() + "." + option.getId(), List.of(table), value -> value instanceof List);
					// Very sadly, we can't add comments to array of tables in night config :(
					// might need to switch to new library one day
					// config.setComment(option.getName(), option.getDescription());
				} catch(Exception e)
				{
					CensorCraft.LOGGER.warn("An error occurred defining the config for punishment '{}'", option.getId(), e);
				}
			}
		}
		
		// spec.define("punishments", punishments);
	}
}
