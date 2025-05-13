package com.supasulley.censorcraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.supasulley.censorcraft.gui.ConfigScreen;
import com.supasulley.censorcraft.network.WordPacket;

import io.github.freshsupasulley.JScribe;
import io.github.freshsupasulley.NoMicrophoneException;
import io.github.freshsupasulley.Transcriptions;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPauseChangeEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = CensorCraft.MODID, value = Dist.CLIENT)
public class ClientCensorCraft {
	
	private static final long OVERLAP_LENGTH = 200;
	
	// JScribe
	private static JScribe controller;
	private static Path model;
	private static long audioContextLength;
	
	private static boolean inGame, paused;
	
	// Packets
	public static final long HEARTBEAT_TIME = 30000, HEARTBEAT_SAFETY_NET = 5000;
	private static long lastWordPacket;
	
	// GUI
	public static final int PADDING = 5;
	private static final long GUI_TIMEOUT = 10000;
	private static final int TRANSCRIPTION_LENGTH = 100;
	
	public static MutableComponent GUI_TEXT;
	public static float JSCRIBE_VOLUME;
	public static boolean SPEAKING;
	
	private static long lastTranscriptionUpdate;
	private static String transcription;
	private static int recordings;
	
	static
	{
		MinecraftForge.registerConfigScreen((minecraft, screen) -> new ConfigScreen(minecraft, screen));
		
		final String tinyModel = "tiny.en";
		
		// If we don't have tiny.en in the models directory yet (probably one of the first times booting this mod)
		if(!hasModel(tinyModel))
		{
			try
			{
				Path model = getModelPath(tinyModel);
				CensorCraft.LOGGER.info("Copying built-in model to {}", model);
				
				Files.copy(CensorCraft.class.getClassLoader().getResourceAsStream(tinyModel + ".bin"), model, StandardCopyOption.REPLACE_EXISTING);
				CensorCraft.LOGGER.info("Put built-in model at {}", tinyModel);
			} catch(IOException e)
			{
				CensorCraft.LOGGER.error("Failed to extract fallback model", e);
				System.exit(1);
			}
		}
		
		controller = new JScribe(CensorCraft.LOGGER);
	}
	
	public static Path getModelDir()
	{
		Path models = FMLPaths.CONFIGDIR.get().resolve("censorcraft/models");
		
		try
		{
			Files.createDirectories(models);
		} catch(IOException e)
		{
			CensorCraft.LOGGER.error("Failed to create model directory {}", models, e);
		}
		
		return models;
	}
	
	public static Path getModelPath(String modelName)
	{
		return getModelDir().resolve(modelName + ".bin");
	}
	
	// needs to someday include tiny.en (built in model)
	public static boolean hasModel(String modelName)
	{
		return getModelPath(modelName).toFile().exists();
	}
	
	private static void startJScribe()
	{
		if(controller.isRunning())
		{
			CensorCraft.LOGGER.debug("Ignoring start request, JScribe is already running");
			return;
		}
		
		// Model might have changed, might as well reinstantiate
		try
		{
			controller.start(model, Config.Client.PREFERRED_MIC.get(), Config.Client.LATENCY.get(), audioContextLength - Config.Client.LATENCY.get() + OVERLAP_LENGTH, Config.Client.VAD.get(), Config.Client.DENOISE.get());
			
			MutableComponent component = Component.literal("Now listening to ");
			component.append(Component.literal(controller.getActiveMicrophone().getName() + ". ").withStyle(style -> style.withBold(true)));
			// Puts above inventory bar
			Minecraft.getInstance().getChatListener().handleSystemMessage(component, true);
		} catch(NoMicrophoneException e)
		{
			CensorCraft.LOGGER.error("No microphones found", e);
		} catch(IOException e)
		{
			CensorCraft.LOGGER.error("Failed to start JScribe", e);
		}
	}
	
	private static void stopJScribe()
	{
		controller.stop();
		setGUIText(Component.literal("Stopped recording."));
	}
	
	@SubscribeEvent
	public static void onPause(ClientPauseChangeEvent.Post event)
	{
		// This event is so weird. Detects pausing like every tick regardless if you're in game
		if(inGame && event.isPaused() != paused)
		{
			paused = event.isPaused();
			CensorCraft.LOGGER.info("Paused: {}", paused);
			
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
	
	// its expected that SetupPacket will be consumed before this
	@SubscribeEvent
	public static void onJoinWorld(ClientPlayerNetworkEvent.LoggingIn event)
	{
		inGame = true;
		startJScribe();
	}
	
	@SubscribeEvent
	public static void onLeaveWorld(ClientPlayerNetworkEvent.LoggingOut event)
	{
		inGame = false;
		stopJScribe();
	}
	
	/**
	 * Every (client) tick, JScribe should be running. If it's not, we need to signal that to the user.
	 * 
	 * @param event {@linkplain LevelTickEvent}
	 */
	@SubscribeEvent
	public static void onLevelTick(LevelTickEvent event)
	{
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
		
		// If we're supposed to be recording AND we're not paused AND the player is alive
		if(inGame && !paused && Minecraft.getInstance().player.isAlive()) // is this gonna throw nulls
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
				
				CensorCraft.LOGGER.info("Sending \"{}\"", text);
				CensorCraft.channel.send(new WordPacket(text), PacketDistributor.SERVER.noArg());
				
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
			CensorCraft.LOGGER.info("Sending heartbeat (paused: {})", paused);
			lastWordPacket = System.currentTimeMillis();
			CensorCraft.channel.send(new WordPacket(""), PacketDistributor.SERVER.noArg());
		}
	}
	
	private static void setGUIText(MutableComponent component)
	{
		GUI_TEXT = component;
	}
	
	/**
	 * Restarts JScribe. The model needs to exist!
	 * 
	 * @param model name of model in model dir
	 */
	public static void setup(Path model, long audioContextLength)
	{
		stopJScribe();
		ClientCensorCraft.model = model;
		ClientCensorCraft.audioContextLength = audioContextLength;
		startJScribe();
	}
}
