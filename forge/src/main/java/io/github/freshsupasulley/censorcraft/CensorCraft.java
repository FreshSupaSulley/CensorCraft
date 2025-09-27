package io.github.freshsupasulley.censorcraft;

import com.mojang.logging.LogUtils;
import io.github.freshsupasulley.censorcraft.api.CensorCraftPlugin;
import io.github.freshsupasulley.censorcraft.api.ForgeCensorCraftPlugin;
import io.github.freshsupasulley.censorcraft.api.events.Event;
import io.github.freshsupasulley.censorcraft.api.events.PluginRegistration;
import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;
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
import java.util.List;
import java.util.function.Consumer;

@Mod(CensorCraft.MODID)
public class CensorCraft {
	
	public static final String MODID = "censorcraft";
	public static final Logger LOGGER = LogUtils.getLogger();
	
	// Packets
	public static SimpleChannel channel;
	
	/** Used for the plugins! */
	public static EventHandler events;
	public static List<Class<? extends Punishment>> punishments = new ArrayList<>();
	
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
		PluginRegistration registration = new PluginRegistration() {
			
			@Override
			public <T extends Event> void registerEvent(Class<T> eventClass, Consumer<T> onEvent)
			{
				eventBuilder.addEvent(eventClass, onEvent);
			}
			
			@Override
			public void registerPunishment(Class<? extends Punishment> punishment)
			{
				punishments.add(punishment);
			}
		};
		
		for(CensorCraftPlugin plugin : plugins)
		{
			LOGGER.info("Registering events for CensorCraft plugin '{}'", plugin.getPluginId());
			
			try
			{
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
							plugins.add(plugin);
						}
					} catch(Throwable e)
					{
						CensorCraft.LOGGER.warn("Failed to load plugin '{}'", annotationData.memberName(), e);
					}
				}
			});
		});
		
		return plugins;
	}
}
