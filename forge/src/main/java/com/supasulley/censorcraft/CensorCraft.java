package com.supasulley.censorcraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.supasulley.censorcraft.gui.ConfigScreen;
import com.supasulley.censorcraft.network.Trie;
import com.supasulley.censorcraft.network.WordPacket;

import io.github.freshsupasulley.JScribe;
import io.github.freshsupasulley.NoMicrophoneException;
import io.github.freshsupasulley.Transcriptions;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory;
import net.minecraftforge.client.event.ClientPauseChangeEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
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
	
	private static final long AUDIO_CONTEXT_LENGTH = 3000, OVERLAP_LENGTH = 200;
	
	private static JScribe controller;
	private boolean inGame, paused;
	
	// Packets
	public static final long HEARTBEAT_TIME = 30000, HEARTBEAT_SAFETY_NET = 5000;
	private SimpleChannel channel;
	private long lastWordPacket;
	
	// Server only
	public static Trie tabooTree;
	
	// GUI
	private static final long GUI_TIMEOUT = 10000;
	private static final int TRANSCRIPTION_LENGTH = 100;
	
	public static MutableComponent GUI_TEXT;
	public static float JSCRIBE_VOLUME;
	public static boolean SPEAKING;
	
	private long lastTranscriptionUpdate;
	private String transcription;
	private int recordings;
	
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
		if(controller.isRunning())
		{
			LOGGER.debug("Ignoring start request, JScribe is already running");
			return;
		}
		
		try
		{
			controller.start(Config.Client.PREFERRED_MIC.get(), Config.Client.LATENCY.get(), AUDIO_CONTEXT_LENGTH - Config.Client.LATENCY.get() + OVERLAP_LENGTH, Config.Client.VAD.get(), Config.Client.DENOISE.get());
			
			MutableComponent component = Component.literal("Now listening to ");
			component.append(Component.literal(controller.getActiveMicrophone().getName() + ". ").withStyle(style -> style.withBold(true)));
			// Puts above inventory bar
			Minecraft.getInstance().getChatListener().handleSystemMessage(component, true);
		} catch(NoMicrophoneException e)
		{
			LOGGER.error("No microphones found", e);
		} catch(IOException e)
		{
			LOGGER.error("Failed to start JScribe", e);
		}
	}
	
	private void stopJScribe()
	{
		controller.stop();
		setGUIText(Component.literal("Stopped recording."));
	}
	
	@SubscribeEvent
	public void onPause(ClientPauseChangeEvent.Post event)
	{
		// This event is so weird. Detects pausing like every tick regardless if you're in game
		if(inGame && event.isPaused() != paused)
		{
			paused = event.isPaused();
			LOGGER.info("Paused: {}", paused);
			
			if(paused)
			{
				stopJScribe();
			}
			else
			{
				startJScribe();
			}
		}
	}
	
	@SubscribeEvent
	public void onJoinWorld(ClientPlayerNetworkEvent.LoggingIn event)
	{
		inGame = true;
		LOGGER.debug("Client logged out event fired");
		startJScribe();
	}
	
	@SubscribeEvent
	public void onLeaveWorld(ClientPlayerNetworkEvent.LoggingOut event)
	{
		inGame = false;
		LOGGER.debug("Client logged out event fired");
		stopJScribe();
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
			
		// Update bar height, "smoothly"
		// Also give the volume a lil boost
		// Use mth.lerp
		// JSCRIBE_VOLUME = lerp(Math.clamp(controller.getAudioLevel() * 1.5f, 0, 1), controller.getAudioLevel(), 0.1f);
		JSCRIBE_VOLUME = Math.clamp(controller.getAudioLevel() * 1.5f, 0, 1);
		SPEAKING = controller.voiceDetected();
		
		// If the mic source changed, user restarted it, etc.
		if(ConfigScreen.restart())
		{
			// setGUIText(Component.literal("Restarting...").withStyle(style -> style.withBold(true)));
			stopJScribe();
			startJScribe();
		}
		
		// If we're supposed to be recording AND we're not paused
		if(inGame && !paused)
		{
			if(!controller.isRunning())
			{
				setGUIText(Component.literal("CensorCraft not running!\n").withStyle(style -> style.withBold(true).withColor(0xFF0000)).append(Component.literal("Rejoin world or click Restart in the mod config menu. If this persists, check logs.").withStyle(style -> style.withBold(false).withColor(0xFFFFFF))));
				return;
			}
			else if(controller.noAudio())
			{
				setGUIText(Component.literal("Not receiving audio!\n").withStyle(style -> style.withBold(true).withColor(0xFF0000)).append(Component.literal("Try changing the microphone in the mod config menu.").withStyle(style -> style.withBold(false).withColor(0xFFFFFF))));
				return;
			}
			
			// Beyond this point, we need it to be running and actively transcribing
			// If it's not blank, send it
			// Send empty packet anyways if we need to keep up with the heartbeat
			Transcriptions results = controller.getTranscriptions();
			
			if(!results.isEmpty())
			{
				String raw = results.getRawString();
				
				// Show end instead of front
				final int newLength = Math.min(raw.length(), TRANSCRIPTION_LENGTH);
				String text = "";
				
				// If we had to splice it, add an ellipsis
				if(raw.length() > TRANSCRIPTION_LENGTH)
				{
					text += "... ";
				}
				
				text += raw.substring(raw.length() - newLength);
				
				lastWordPacket = System.currentTimeMillis();
				
				LOGGER.info("Sending \"{}\"", text);
				channel.send(new WordPacket(text), PacketDistributor.SERVER.noArg());
				
				lastTranscriptionUpdate = System.currentTimeMillis();
				transcription = text;
				recordings = results.getTotalRecordings();
			}
			
			// Show transcriptions only if necessary
			if(Config.Client.SHOW_TRANSCRIPTION.get() && System.currentTimeMillis() - lastTranscriptionUpdate < GUI_TIMEOUT)
			{
				MutableComponent component = Component.literal(transcription + "\n").withColor(0xFFFFFF);
				
				if(Config.Client.SHOW_DELAY.get())
				{
					component.append(Component.literal(String.format("%.1f", controller.getTimeBehind() / 1000f) + "s behind (" + recordings + " recording" + (recordings != 1 ? "s" : "") + ")").withColor(0xAAAAAA));
				}
				
				setGUIText(component);
			}
			else
			{
				setGUIText(Component.empty());
			}
		}
		
		// Heartbeat
		if(System.currentTimeMillis() - lastWordPacket >= HEARTBEAT_TIME - HEARTBEAT_SAFETY_NET)
		{
			LOGGER.info("Sending heartbeat (paused: {})", paused);
			lastWordPacket = System.currentTimeMillis();
			channel.send(new WordPacket(""), PacketDistributor.SERVER.noArg());
		}
	}
	
	private void setGUIText(MutableComponent component)
	{
		GUI_TEXT = component;
	}
}
