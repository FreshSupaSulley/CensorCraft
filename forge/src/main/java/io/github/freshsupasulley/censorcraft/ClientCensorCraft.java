package io.github.freshsupasulley.censorcraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import io.github.freshsupasulley.JScribe;
import io.github.freshsupasulley.Model;
import io.github.freshsupasulley.NoMicrophoneException;
import io.github.freshsupasulley.Transcriptions;
import io.github.freshsupasulley.censorcraft.config.Config;
import io.github.freshsupasulley.censorcraft.gui.ConfigScreen;
import io.github.freshsupasulley.censorcraft.gui.DownloadScreen;
import io.github.freshsupasulley.censorcraft.network.WordPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPauseChangeEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
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
	public static boolean librariesLoaded;
	private static JScribe controller;
	private static Path model;
	private static long audioContextLength;
	
	private static boolean loggedIn, paused, startJScribeAttempt;
	
	// Packets
	public static final long HEARTBEAT_TIME = 30000, HEARTBEAT_SAFETY_NET = 5000;
	private static long lastWordPacket;
	
	// Setup
	private static boolean disconnectFlag;
	private static String requestedModel;
	
	// GUI
	public static final int PADDING = 5;
	private static final int MAX_TRANSCRIPTION_LENGTH = 60;
	
	public static MutableComponent GUI_TEXT;
	public static float JSCRIBE_VOLUME;
	public static boolean SPEAKING;
	
	private static String transcription;
	private static int recordings;
	
	// Looks like we're not bundling models in. Too big of a jar file
//	static
//	{
//		MinecraftForge.registerConfigScreen((minecraft, screen) -> new ConfigScreen(minecraft, screen));
//		
//		final String tinyModel = "tiny.en";
//		
//		// If we don't have tiny.en in the models directory yet (probably one of the first times booting this mod)
//		if(!hasModel(tinyModel))
//		{
//			try
//			{
//				Path model = getModelPath(tinyModel);
//				CensorCraft.LOGGER.info("Copying built-in model to {}", model);
//				
//				Files.copy(CensorCraft.class.getClassLoader().getResourceAsStream(tinyModel + ".bin"), model, StandardCopyOption.REPLACE_EXISTING);
//				CensorCraft.LOGGER.info("Put built-in model at {}", tinyModel);
//			} catch(IOException e)
//			{
//				CensorCraft.LOGGER.error("Failed to extract fallback model", e);
//				System.exit(1);
//			}
//		}
//	}
	
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
		if(controller != null && controller.isInUse())
		{
			CensorCraft.LOGGER.debug("Ignoring start request, JScribe is already running");
			return;
		}
		
		JScribe.Builder builder = new JScribe.Builder().setLogger(CensorCraft.LOGGER).warmUpModel();
		
		if(Config.Client.USE_VULKAN.get())
		{
			CensorCraft.LOGGER.warn("Enabling Vulkan");
			builder.useVulkan();
		}
		
		controller = builder.build();
		
		// Reset debug params
		recordings = 0;
		transcription = null;
		
		// Model might have changed, might as well reinstantiate
		try
		{
			librariesLoaded = true;
			controller.start(model, Config.Client.PREFERRED_MIC.get(), Config.Client.LATENCY.get(), Math.max(0, audioContextLength - Config.Client.LATENCY.get()) + OVERLAP_LENGTH, Config.Client.VAD.get(), Config.Client.DENOISE.get());
		} catch(NoMicrophoneException e)
		{
			CensorCraft.LOGGER.error("No microphones found", e);
		} catch(Exception e)
		{
			CensorCraft.LOGGER.error("Failed to start JScribe", e);
		}
	}
	
	private static void stopJScribe()
	{
		startJScribeAttempt = false;
		
		if(controller == null)
		{
			// CensorCraft.LOGGER.error("Tried to stop JScribe when controller is not initialized", new Throwable()); // get the stacktrace if this happens
			return;
		}
		
		controller.stop();
		// setGUIText(Component.literal("Stopped recording."));
	}
	
	@SubscribeEvent
	public static void screenEvent(ScreenEvent.Opening event)
	{
		// I don't need to do this instanceof check but it makes me feel better
		if(event.getNewScreen() instanceof DisconnectedScreen && disconnectFlag)
		{
			disconnectFlag = false;
			
			try
			{
				// Probably ok that this happens in the main thread
				Model model = JScribe.getModelInfo(requestedModel);
				
				if(model == null)
				{
					event.setNewScreen(errorScreen("Server requested a model that doesn't exist (" + requestedModel + ")", "Ask the server owner to fix the config"));
				}
				else
				{
					event.setNewScreen(new PopupScreen.Builder(new TitleScreen(), Component.literal("Missing model")).setMessage(Component.literal("This server requires a transcription model to play (").append(Component.literal(requestedModel + ", " + model.getSizeFancy()).withStyle(Style.EMPTY.withBold(true))).append(")\n\nDownload the model?")).addButton(CommonComponents.GUI_YES, (screen) ->
					{
						Minecraft.getInstance().setScreen(new DownloadScreen(model));
					}).addButton(CommonComponents.GUI_NO, PopupScreen::onClose).build());
				}
			} catch(IOException e)
			{
				event.setNewScreen(errorScreen("Failed to get model info", e));
			}
		}
	}
	
	public static Screen errorScreen(String title, String reason)
	{
		// return new ErrorScreen(Component.literal(title), Component.literal(reason));
		return new PopupScreen.Builder(new TitleScreen(), Component.literal(title).withColor(-65536)).setMessage(Component.literal(reason)).addButton(CommonComponents.GUI_OK, PopupScreen::onClose).build();
	}
	
	public static Screen errorScreen(String title, Throwable t)
	{
		CensorCraft.LOGGER.error(title, t);
		return errorScreen(title, t.getLocalizedMessage() == null ? t.getClass().toString() : t.getLocalizedMessage());
	}
	
	/**
	 * This only has an effect in singleplayer worlds. When connected to a server, pausing on the client side doesn't stop transcription.
	 * 
	 * @param event
	 */
	@SubscribeEvent
	public static void onPause(ClientPauseChangeEvent.Post event)
	{
		// This event is so weird. Detects pausing like every tick regardless if you're in game
		if(loggedIn && event.isPaused() != paused)
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
		CensorCraft.LOGGER.info("LoggingIn event fired");
		loggedIn = true;
		startJScribe();
	}
	
	@SubscribeEvent
	public static void onLeaveWorld(ClientPlayerNetworkEvent.LoggingOut event)
	{
		CensorCraft.LOGGER.info("LoggingOut event fired");
		loggedIn = false;
		stopJScribe();
	}
	
	//
	// @SubscribeEvent
	// public static void onRespawn(PlayerEvent.PlayerRespawnEvent event)
	// {
	// CensorCraft.LOGGER.info("Player respawned");
	// startJScribe();
	// }
	//
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
		
		// no clue if .player can be null but im compensating for it anyways
		@SuppressWarnings("resource")
		boolean playerAlive = Optional.ofNullable(Minecraft.getInstance().player).map(LocalPlayer::isAlive).orElse(false);
		
		// If player is dead, don't run JScribe
		if(!playerAlive && controller.isRunning())
		{
			stopJScribe();
		}
		
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
		
		// If we're supposed to be recording
		if(loggedIn && !paused && playerAlive)
		{
			// Only try to init one time so we can indicate if there's something wrong
			if(!startJScribeAttempt)
			{
				startJScribeAttempt = true;
				startJScribe();
			}
			
			if(controller.isInitializing())
			{
				setGUIText(Component.literal("Initializing..."));
			}
			else if(!controller.isRunning())
			{
				setGUIText(Component.literal("CensorCraft not running!\n").withStyle(style -> style.withBold(true).withColor(0xFF0000)).append(Component.literal("Rejoin world or click Restart in the mod config menu. If this persists, check logs.").withStyle(style -> style.withBold(false).withColor(0xAAAAAA))));
				return;
			}
			else if(controller.noAudio())
			{
				setGUIText(Component.literal("Not receiving audio!\n").withStyle(style -> style.withBold(true).withColor(0xFF0000)).append(Component.literal("Try changing the microphone in the mod config menu.").withStyle(style -> style.withBold(false).withColor(0xAAAAAA))));
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
				final int newLength = Math.min(raw.length(), MAX_TRANSCRIPTION_LENGTH);
				String text = "";
				
				// If we had to splice it, add an ellipsis
				if(raw.length() > MAX_TRANSCRIPTION_LENGTH)
				{
					text += "... ";
				}
				
				text += raw.substring(raw.length() - newLength);
				
				CensorCraft.LOGGER.info("Sending \"{}\"", text);
				lastWordPacket = System.currentTimeMillis();
				CensorCraft.channel.send(new WordPacket(text), PacketDistributor.SERVER.noArg());
				
				transcription = text;
				recordings = results.getTotalRecordings();
			}
			
			MutableComponent component = Component.empty();
			
			// 15000 warning
			if(controller.getTimeBehind() > 15000)
			{
				component.append(Component.literal("CensorCraft is far behind\n").withStyle(style -> style.withBold(true).withColor(0xFF0000)).append(Component.literal("Consider raising transcription latency\n").withStyle(style -> style.withBold(false).withColor(0xAAAAAA))));
			}
			
			// Show transcriptions only if necessary
			if(Config.Client.SHOW_TRANSCRIPTION.get())
			{
				if(transcription != null && !transcription.isBlank())
				{
					component.append(Component.literal(transcription + "\n").withColor(0xFFFFFF));
				}
			}
			
			if(Config.Client.DEBUG.get())
			{
				component.append(Component.literal(String.format("%.1f", controller.getTimeBehind() / 1000f) + "s behind\n").withColor(0xAAAAAA));
				component.append(Component.literal("Latency: " + Config.Client.LATENCY.get() + "\n"));
				component.append(Component.literal("Last transcribed " + recordings + " recording" + (recordings != 1 ? "s" : "") + "\n")).withColor(0xAAAAAA);
				component.append(Component.literal(controller.getTranscriptionBacklog() + " samples queued\n"));
				component.append(Component.literal("Using " + model.getFileName() + " model\n"));
			}
			// else
			// {
			// setGUIText(Component.empty());
			// }
			
			setGUIText(component);
		}
		// If we're NOT supposed to be running
		else
		{
			if(controller.isShuttingDown())
			{
				setGUIText(Component.literal("Stopping..."));
			}
			else if(!controller.isInUse())
			{
				setGUIText(Component.literal("Stopped"));
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
		// stopJScribe();
		ClientCensorCraft.model = model;
		ClientCensorCraft.audioContextLength = audioContextLength;
		startJScribe();
	}
	
	public static void requestModelDownload(String model)
	{
		disconnectFlag = true;
		requestedModel = model;
	}
	
	public static void punished()
	{
		if(controller != null && controller.isRunning())
		{
			controller.reset();
		}
	}
}
