package io.github.freshsupasulley.censorcraft.common;

import com.mojang.logging.LogUtils;
import io.github.freshsupasulley.censorcraft.api.CensorCraftPlugin;
import io.github.freshsupasulley.censorcraft.api.events.Event;
import io.github.freshsupasulley.censorcraft.api.events.PluginRegistration;
import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;
import io.github.freshsupasulley.censorcraft.common.config.punishments.*;
import io.github.freshsupasulley.censorcraft.common.network.IPacket;
import io.github.freshsupasulley.censorcraft.common.plugins.EventHandler;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public abstract class CensorCraft {
	
	public static final String MODID = "censorcraft";
	public static final Logger LOGGER = LogUtils.getLogger();
	
	// Used for the plugins
	public static EventHandler events;
	public static Map<CensorCraftPlugin, Set<Punishment>> pluginPunishments = new HashMap<>();
	
	/** For validating plugin and punishment IDs to ensure they meet the TOML bare key standard */
	private static final Pattern TOML_BARE_KEY = Pattern.compile("^[A-Za-z0-9_-]+$");
	
	// For access to things that need to be defined in the subclass
	public static CensorCraft INSTANCE;
	
	// Define our built-in plugin
	private final CensorCraftPlugin defaultPlugin = new CensorCraftPlugin() {
		
		@Override
		public String getPluginId()
		{
			// maybe change
			return "censorcraft";
		}
		
		@Override
		public void register(PluginRegistration r)
		{
			r.registerPunishment(Commands.class);
			r.registerPunishment(Crash.class);
			r.registerPunishment(Dimension.class);
			r.registerPunishment(Entities.class);
			r.registerPunishment(Explosion.class);
			r.registerPunishment(Ignite.class);
			r.registerPunishment(Kill.class);
			r.registerPunishment(Lightning.class);
			r.registerPunishment(MobEffects.class);
			r.registerPunishment(Teleport.class);
		}
	};
	
	public CensorCraft()
	{
		// Plugin shenanigans
		LOGGER.info("Loading CensorCraft plugins");
		List<CensorCraftPlugin> plugins = new ArrayList<>();
		plugins.add(defaultPlugin);
		
		for(CensorCraftPlugin plugin : findPlugins())
		{
			// Ensure the ID isn't empty
			if(!isValidTomlKey(plugin.getPluginId()))
			{
				LOGGER.warn("Plugin '" + plugin.getPluginId() + "' fails TOML bare key regex");
			}
			// Check if someone already declared a plugin with this ID already
			else if(plugins.stream().anyMatch(sample -> sample.getPluginId().equals(plugin.getPluginId())))
			{
				LOGGER.warn("Another plugin declared with ID '{}' was already loaded", plugin.getPluginId());
			}
			else
			{
				LOGGER.info("Found CensorCraft plugin: '{}'", plugin.getPluginId());
				plugins.add(plugin);
			}
		}
		
		LOGGER.info("Found {} plugin(s)", plugins.size());
		
		EventHandler.EventHandlerBuilder eventBuilder = new EventHandler.EventHandlerBuilder(LOGGER);
		
		for(CensorCraftPlugin plugin : plugins)
		{
			LOGGER.info("Registering events for CensorCraft plugin '{}'", plugin.getPluginId());
			pluginPunishments.put(plugin, new HashSet<>());
			
			try
			{
				PluginRegistration registration = new PluginRegistration() {
					
					@Override
					public <T extends Event> void registerEvent(Class<T> eventClass, Consumer<T> onEvent)
					{
						eventBuilder.addEvent(eventClass, onEvent);
					}
					
					// this might need to be done on the server side only??
					// wait no because we need to be able to run client side punishments anyways so the client HAS to know about them
					@Override
					public void registerPunishment(Class<? extends Punishment> clazz)
					{
						Punishment punishment;
						
						try
						{
							LOGGER.info("Attempting to instantiate {}", clazz);
							punishment = Punishment.newInstance(clazz);
						} catch(RuntimeException e)
						{
							// This is the first place plugin-defined punishments are attempted to be instantiated, so this log is necessary
							LOGGER.warn("Can't instantiate punishment '{}'. The punishment must have a default constructor with no args!", clazz.getName(), e);
							// Should be caught in parent try catch
							throw e;
						}
						
						// Now that it's guaranteed to be non-null, check the ID
						if(!isValidTomlKey(punishment.getId()))
						{
							throw new IllegalStateException("Punishment ID defined at " + clazz + " fails TOML bare key regex");
						}
						
						// This will throw an error if a punishment was already declared with this name
						pluginPunishments.get(plugin).add(punishment);
					}
				};
				
				plugin.register(registration);
			} catch(Throwable e)
			{
				LOGGER.warn("Registration failed for CensorCraft plugin '{}'", plugin.getPluginId(), e);
			}
		}
		
		events = eventBuilder.build();
	}
	
	protected abstract List<CensorCraftPlugin> findPlugins();
	
	private static boolean isValidTomlKey(String key)
	{
		return TOML_BARE_KEY.matcher(key).matches();
	}
	
	public abstract void sendToPlayer(IPacket punishedPacket, ServerPlayer player);
	
	public abstract void sendToServer(IPacket wordPacket);
}
