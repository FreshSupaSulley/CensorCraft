package io.github.freshsupasulley.censorcraft;

import com.mojang.logging.LogUtils;
import io.github.freshsupasulley.censorcraft.api.CensorCraftPlugin;
import io.github.freshsupasulley.censorcraft.api.ForgeCensorCraftPlugin;
import io.github.freshsupasulley.censorcraft.api.events.Event;
import io.github.freshsupasulley.censorcraft.api.events.PluginRegistration;
import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;
import io.github.freshsupasulley.censorcraft.config.punishments.*;
import io.github.freshsupasulley.censorcraft.network.PunishedPacket;
import io.github.freshsupasulley.censorcraft.network.SetupPacket;
import io.github.freshsupasulley.censorcraft.network.WordPacket;
import io.github.freshsupasulley.plugins.EventHandler;
import io.github.freshsupasulley.plugins.EventHandler.EventHandlerBuilder;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Mod(CensorCraft.MODID)
public class CensorCraft {
	
	public static final String MODID = "censorcraft";
	public static final Logger LOGGER = LogUtils.getLogger();
	
	// Packets
	public static SimpleChannel channel;
	
	/** Used for the plugins */
	public static EventHandler events;
	public static Map<String, PunishmentRegistry> pluginPunishments = new HashMap();
	
	// Define our built-in plugin
	private CensorCraftPlugin defaultPlugin = new CensorCraftPlugin() {
		
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
	
	public CensorCraft(FMLJavaModLoadingContext context)
	{
		var modBusGroup = context.getModBusGroup();
		FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::commonSetup);
	}
	
	// Packet communication setup
	private void commonSetup(FMLCommonSetupEvent event)
	{
		// ig just bump this with each new major (and thus incompatible with the last) update?
		final int protocolVersion = 3;
		
		event.enqueueWork(() ->
		{
			channel = ChannelBuilder.named(CensorCraft.MODID).networkProtocolVersion(protocolVersion).simpleChannel();
			
			channel.configuration().clientbound().addMain(SetupPacket.class, SetupPacket.CODEC, SetupPacket::consume);
			channel.play().serverbound().addMain(WordPacket.class, WordPacket.CODEC, WordPacket::consume);
			channel.play().clientbound().addMain(PunishedPacket.class, PunishedPacket.CODEC, PunishedPacket::consume);
			
			channel.build();
		});
		
		// Plugin schenanigans
		CensorCraft.LOGGER.info("Loading CensorCraft plugins");
		var plugins = loadPlugins();
		CensorCraft.LOGGER.info("Found {} plugin(s)", plugins.size());
		
		EventHandlerBuilder eventBuilder = new EventHandlerBuilder(CensorCraft.LOGGER);
		
		for(CensorCraftPlugin plugin : plugins)
		{
			LOGGER.info("Registering events for CensorCraft plugin '{}'", plugin.getPluginId());
			
			// Check if someone already declared a plugin with this ID already
			if(pluginPunishments.containsKey(plugin.getPluginId()))
			{
				CensorCraft.LOGGER.error("2 or more plugins declared as '{}' conflict with each other. Only the first one read will be used", plugin.getPluginId());
				continue;
			}
			
			pluginPunishments.put(plugin.getPluginId(), new PunishmentRegistry());
			
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
							punishment = Punishment.newInstance(clazz);
						} catch(RuntimeException e)
						{
							// This is the first place plugin-defined punishments are attempted to be instantiated, so this log is necessary
							CensorCraft.LOGGER.warn("Can't instantiate punishment '{}'. The punishment must have a default constructor with no args!", clazz.getName(), e);
							// Should be caught in parent try catch
							throw e;
						}
						
						// Now that it's guaranteed to be non-null, check the ID
						if(punishment.getId().isBlank())
						{
							throw new IllegalStateException("Punishment ID cannot be blank for " + clazz);
						}
						
						// This will throw an error if a punishment was already declared with this name
						pluginPunishments.get(plugin.getPluginId()).register(punishment);
					}
				};
				
				plugin.register(registration);
			} catch(Exception e)
			{
				LOGGER.warn("Registration failed for CensorCraft plugin '{}'", plugin.getPluginId(), e);
			}
		}
		
		events = eventBuilder.build();
	}
	
	public List<CensorCraftPlugin> loadPlugins()
	{
		List<CensorCraftPlugin> plugins = new ArrayList<>();
		// Load our default plugin
		plugins.add(defaultPlugin);
		
		ModList.get().getAllScanData().forEach(scan ->
		{
			scan.getAnnotations().forEach(annotationData ->
			{
				if(annotationData.annotationType().getClassName().equals(ForgeCensorCraftPlugin.class.getName()))
				{
					try
					{
						Class<?> clazz = Class.forName(annotationData.memberName());
						
						if(CensorCraftPlugin.class.isAssignableFrom(clazz))
						{
							CensorCraftPlugin plugin = (CensorCraftPlugin) clazz.getDeclaredConstructor().newInstance();
							
							// Ensure the ID isn't empty
							if(plugin.getPluginId().isBlank())
							{
								throw new IllegalStateException("Plugin ID cannot be blank for " + clazz);
							}
							
							plugins.add(plugin);
						}
					} catch(Exception e)
					{
						CensorCraft.LOGGER.warn("Failed to load plugin '{}'", annotationData.memberName(), e);
					}
				}
			});
		});
		
		return plugins;
	}
}
