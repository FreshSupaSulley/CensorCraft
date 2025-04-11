package com.supasulley.censorcraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.supasulley.censorcraft.gui.ConfigScreen;
import com.supasulley.jscribe.JScribe;
import com.supasulley.network.WordPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(CensorCraft.MODID)
public class CensorCraft {
	
	public static final String MODID = "censorcraft";
	public static final Logger LOGGER = LogUtils.getLogger();
	
	private static JScribe controller;
	private boolean playerAlive;
	
	// Packets
	public static final long HEARTBEAT_TIME = 30000, HEARTBEAT_SAFETY_NET = 5000;
	private SimpleChannel channel;
	private long lastWordPacket;
	
	// Server only
	public static Trie tabooTree;
	
	// GUI
	private static final long DEGRADE_GUI = 10000;
	public static MutableComponent GUI_TEXT;
	public static float JSCRIBE_VOLUME;
	private static long lastMessage;
	
	public CensorCraft(FMLJavaModLoadingContext context)
	{
		// Forbidden words are defined at the server level
		Config.register(context);
		
		// Register ourselves for server and other game events we are interested in
		MinecraftForge.EVENT_BUS.register(this);
		context.getModEventBus().addListener(this::clientSetup);
		context.getModEventBus().addListener(this::commonSetup);
		
		// context.getContainer().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> "", (remoteVersion,
		// isFromServer) -> new ConfigScreen());
		context.getContainer().registerExtensionPoint(ConfigScreenFactory.class, () -> new ConfigScreenFactory((minecraft, screen) -> new ConfigScreen(minecraft, screen)));
	}
	
	// Mod Event Bus events
	public void commonSetup(FMLCommonSetupEvent event)
	{
		final int protocolVersion = 1;
		
		event.enqueueWork(() ->
		{
			channel = ChannelBuilder.named(CensorCraft.MODID).networkProtocolVersion(protocolVersion).simpleChannel();
			channel.messageBuilder(WordPacket.class, NetworkDirection.PLAY_TO_SERVER).encoder(WordPacket::encode).decoder(WordPacket::decode).consumerMainThread(WordPacket::consume).add();
		});
	}
	
	public void clientSetup(FMLClientSetupEvent event)
	{
		// Some common setup code
		LOGGER.info("Copying model to temp directory");
		
		try
		{
			Path tempZip = Files.createTempFile("model", ".en.bin");
			tempZip.toFile().deleteOnExit();
			Files.copy(CensorCraft.class.getClassLoader().getResourceAsStream("ggml-tiny.en.bin"), tempZip, StandardCopyOption.REPLACE_EXISTING);
			LOGGER.info("Put whisper model at {}", tempZip);
			
			controller = new JScribe(LOGGER, tempZip);
		} catch(IOException e)
		{
			LOGGER.error("Failed to load model");
			e.printStackTrace();
		}
	}
	
	private void startJScribe()
	{
		if(controller.start(Config.Client.PREFERRED_MIC.get(), 750, 1000))
		{
			MutableComponent component = Component.literal("Now listening to ");
			component.append(Component.literal(controller.getActiveMicrophone().getName() + ". ").withStyle(style -> style.withBold(true)));
			
			Minecraft.getInstance().getChatListener().handleSystemMessage(component, true);
//			setGUIText(component);
		}
	}
	
	/**
	 * Handles when the <b>client-side</b> player joins the world (respawns, logs in, moves between dimensions, etc.)
	 * 
	 * <p>
	 * Only applies to the local player, meaning when server players invoke this event, it's ignored.
	 * </p>
	 * 
	 * @param event {@linkplain EntityJoinLevelEvent}
	 */
	@SubscribeEvent
	public void onJoinWorld(EntityJoinLevelEvent event)
	{
		if(!(event.getEntity() instanceof LocalPlayer))
			return;
		
		playerAlive = true;
		startJScribe();
	}
	
	/**
	 * Handles when the player leaves the world (dies, exits, quits, etc.).
	 * 
	 * <p>
	 * If the player moves between dimensions (i.e. travels to the nether), this doesn't get fired.
	 * </p>
	 * 
	 * @param event {@linkplain EntityLeaveLevelEvent}
	 */
	@SubscribeEvent
	public void onLeaveWorld(EntityLeaveLevelEvent event)
	{
		if(!(event.getEntity() instanceof LocalPlayer))
			return;
		
		playerAlive = false;
		
		if(controller.stop())
		{
			setGUIText(Component.literal("Stopped recording."));
		}
	}
	
	/**
	 * Every (client) tick, JScribe should be running. If it's not, we need to signal that to the user.
	 * 
	 * @param event {@linkplain LevelTickEvent}
	 */
	@SubscribeEvent
	public void onLevelTick(LevelTickEvent event)
	{
		// This is only for client ticks
		if(event.side != LogicalSide.CLIENT)
			return;
		
		// Update bar height
		JSCRIBE_VOLUME = controller.getAudioLevel();
		
		// Degrade old GUI messages
		if(System.currentTimeMillis() - lastMessage >= DEGRADE_GUI)
		{
			GUI_TEXT = null;
		}
		
		// If we're supposed to be recording
		if(playerAlive)
		{
			if(!controller.isRunning())
			{
				setGUIText(Component.literal("CensorCraft not running!\n").withStyle(style -> style.withBold(true).withColor(0xFF0000)).append(Component.literal("Rejoin world or click Restart in the mod config menu. If this persists, check logs.").withStyle(style -> style.withBold(false).withColor(0xFFFFFF))));
			}
			else if(controller.isRunningAndNoAudio())
			{
				setGUIText(Component.literal("Not receiving audio!\n").withStyle(style -> style.withBold(true).withColor(0xFF0000)).append(Component.literal("Try changing the microphone in the mod config menu.").withStyle(style -> style.withBold(false).withColor(0xFFFFFF))));
			}
		}
		
		// If the mic source changed, user restarted it, etc.
		if(ConfigScreen.restart())
		{
			setGUIText(Component.literal("Restarting..."));
			controller.stop();
			startJScribe();
		}
		
		// Beyond this point, we need it to be running and actively transcribing
		// UNLESS the player is dead, in which case we can still send heartbeats
		if(!controller.isRunning() || controller.isRunningAndNoAudio() && playerAlive)
			return;
		
		String buffer = controller.getBuffer();
		
		// If it's not blank, send it
		// Send empty packet anyways if we need to keep up with the heartbeat
		if(!buffer.isBlank() || System.currentTimeMillis() - lastWordPacket >= HEARTBEAT_TIME - HEARTBEAT_SAFETY_NET)
		{
			// 0 indicates we're fine
			lastWordPacket = System.currentTimeMillis();
			
			LOGGER.info("Sending \"{}\" (backlog size: {})", buffer, controller.getBacklog());
			channel.send(new WordPacket(buffer), PacketDistributor.SERVER.noArg());
			
			// Only show transcriptions if setting is enabled
			if(Config.Client.SHOW_TRANSCRIPTION.get())
			{
				setGUIText(Component.literal(buffer).withColor(0xFFFFFF));
			}
		}
	}
	
	private void setGUIText(MutableComponent component)
	{
		lastMessage = System.currentTimeMillis();
		GUI_TEXT = component;
	}
	
	/**
	 * @return true if actively recording and transcribing audio, false otherwise
	 */
	public static boolean isRecording()
	{
		return Optional.ofNullable(controller).map(jscribe -> jscribe.isRunning()).orElse(false);
	}
}
