package io.github.freshsupasulley.censorcraft;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.ClientSoundEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import io.github.freshsupasulley.JScribe;
import io.github.freshsupasulley.Model;
import io.github.freshsupasulley.RollingAudioBuffer;
import io.github.freshsupasulley.Transcriptions;
import io.github.freshsupasulley.censorcraft.config.ClientConfig;
import io.github.freshsupasulley.censorcraft.gui.ConfigScreen;
import io.github.freshsupasulley.censorcraft.gui.DownloadScreen;
import io.github.freshsupasulley.censorcraft.network.WordPacket;
import net.minecraft.Util;
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
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;

@ForgeVoicechatPlugin
@Mod.EventBusSubscriber(modid = CensorCraft.MODID, value = Dist.CLIENT)
public class ClientCensorCraft implements VoicechatPlugin {
	
	// JScribe
	public static boolean librariesLoaded; // used for telling the player if we already loaded JScribe natives and they need to restart mc
	
	private static JScribe controller;
	private static Path model;
	private static boolean monitorVoice;
	
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
	public static boolean FORCE_GUI_REFRESH;
	
	// private static StringBuilder transcription = new StringBuilder(MAX_TRANSCRIPTION_LENGTH);
	private static String transcription;
	private static int recordings;
	
	/** MS between samples before the rolling audio buffer is cleared */
	private static final long DRAIN_DELAY = 1000, MIN_SAMPLE_MS = 200;
	
	private static final int SAMPLE_RATE = 48000;
	private static final RollingAudioBuffer ringBuffer = new RollingAudioBuffer(15000, SAMPLE_RATE); // hold a MAX of 15s. Can't even see us needing this much
	private static long lastSample, lastTranscription;
	
	public static void clientSetup(FMLClientSetupEvent event)
	{
		MinecraftForge.registerConfigScreen((minecraft, screen) -> new ConfigScreen(minecraft, screen));
	}
	
	@Override
	public String getPluginId()
	{
		return CensorCraft.MODID;
	}
	
	@Override
	public void initialize(VoicechatApi api)
	{
		CensorCraft.LOGGER.info("INITIALIZED SIMPLE VOICE CHAT API!");
	}
	
	@Override
	public void registerEvents(EventRegistration registration)
	{
		registration.registerEvent(ClientSoundEvent.class, this::onClientSound);
	}
	
	public void onClientSound(ClientSoundEvent event)
	{
		lastSample = System.currentTimeMillis();
		ringBuffer.append(event.getRawAudio());
	}
	
	private static int msToSamples(long ms)
	{
		return (int) ((ms * SAMPLE_RATE) / 1000);
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
		if(!monitorVoice)
		{
			CensorCraft.LOGGER.debug("Not starting JScribe, monitorVoice is disabled");
			return;
		}
		
		if(controller != null && controller.isInUse())
		{
			CensorCraft.LOGGER.debug("Ignoring start request, JScribe is already running");
			return;
		}
		
		// This lets error messages (if any) appear again
		setGUIText(Component.empty());
		
		// apparently not defined anywhere in the api, only the mod, and that's runtime only
		JScribe.Builder builder = new JScribe.Builder(model, SAMPLE_RATE);
		builder.setLogger(CensorCraft.LOGGER);
		// builder.denoise(ClientConfig.DENOISE.get());
		// builder.enableVAD(ClientConfig.VAD_MODE.get());
		// builder.setInputSensitivity(ClientConfig.INPUT_SENSITIVITY.get());
		// builder.setPreferredMicrophone(ClientConfig.PREFERRED_MIC.get());
		builder.warmUpModel();
		
		if(ClientConfig.USE_VULKAN.get())
		{
			CensorCraft.LOGGER.warn("Vulkan enabled in client config");
			builder.useVulkan();
		}
		
		controller = builder.build();
		
		// Reset debug params
		ringBuffer.drain();
		recordings = 0;
		transcription = "";
		
		// Model might have changed, might as well reinstantiate
		try
		{
			librariesLoaded = true;
			controller.start();
		} catch(Exception e)
		{
			CensorCraft.LOGGER.error("Failed to start JScribe", e);
		}
	}
	
	private static void stopJScribe()
	{
		if(!monitorVoice)
		{
			CensorCraft.LOGGER.debug("Not stopping JScribe, monitorVoice is disabled");
			return;
		}
		
		startJScribeAttempt = false;
		
		if(controller == null)
		{
			// CensorCraft.LOGGER.error("Tried to stop JScribe when controller is not initialized", new Throwable()); // get the stacktrace if this happens
			return;
		}
		
		if(controller.isInUse() && !controller.isShuttingDown())
		{
			setGUIText(Component.literal("Stopping transcription..."));
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
					event.setNewScreen(new PopupScreen.Builder(new TitleScreen(), Component.literal("Missing model")).setMessage(Component.literal("This server requires a transcription model to play (").append(Component.literal(requestedModel + ", " + model.getSizeFancy()).withStyle(Style.EMPTY.withBold(true))).append(")\n\nDownload the model?")).addButton(Component.literal("Learn more"), (screen) ->
					{
						Util.getPlatform().openUri(URI.create("https://github.com/FreshSupaSulley/CensorCraft"));
						// screen.onClose();
					}).addButton(CommonComponents.GUI_YES, (screen) ->
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
	
	// @SubscribeEvent
	// public static void onRespawn(PlayerEvent.PlayerRespawnEvent event)
	// {
	// CensorCraft.LOGGER.info("Player respawned");
	// startJScribe();
	// }
	
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
		
		// There is nothing to do if monitorVoice is disabled
		if(!monitorVoice)
		{
			if(ClientConfig.DEBUG.get())
			{
				setGUIText(Component.literal("Transcription is off\n"), true);
			}
			
			return;
		}
		
		// no clue if .player can be null but im compensating for it anyways
		@SuppressWarnings("resource")
		boolean playerAlive = Optional.ofNullable(Minecraft.getInstance().player).map(LocalPlayer::isAlive).orElse(false);
		
		// If player is dead, don't run JScribe
		if(!playerAlive && controller.isRunning())
		{
			stopJScribe();
		}
		
		// If the mic source changed, user restarted it, etc.
		if(ConfigScreen.restart())
		{
			// setGUIText(Component.literal("Restarting...").withStyle(style -> style.withBold(true)));
			stopJScribe();
			startJScribe();
		}
		
		// If we're supposed to be recording
		if(loggedIn && !paused && playerAlive && !controller.isInitializing())
		{
			// Only try to init one time so we can indicate if there's something wrong
			if(!startJScribeAttempt)
			{
				startJScribeAttempt = true;
				startJScribe();
			}
			
			if(!controller.isRunning())
			{
				setGUIText(Component.literal("CensorCraft not running!\n").withStyle(style -> style.withBold(true).withColor(0xFF0000)).append(Component.literal("Rejoin world or click Restart in the mod config menu. If this persists, check logs.").withStyle(style -> style.withBold(false).withColor(0xAAAAAA))), true);
				return;
			}
			
			// If it's been longer than X ms since our last audio packet, scrap the context
			if(System.currentTimeMillis() - lastSample > DRAIN_DELAY)
			{
				// But before we do that, make sure we don't have to transcribe the remains
				if(ringBuffer.getSize() >= msToSamples(MIN_SAMPLE_MS))
				{
					lastTranscription = System.currentTimeMillis();
					controller.transcribe(ringBuffer.getSnapshot());
				}
				
				ringBuffer.drain();
			}
			else
			{
				// How many samples we need for our target buffer size
				long latency = ClientConfig.LATENCY.get();
				
				// If we’ve collected enough and latency says we want another sample
				if(ringBuffer.getSize() >= msToSamples(MIN_SAMPLE_MS) && System.currentTimeMillis() - lastTranscription >= latency)
				{
					lastTranscription = System.currentTimeMillis();
					controller.transcribe(ringBuffer.getSnapshot());
				}
			}
			
			// Beyond this point, we need it to be running and actively transcribing
			// If it's not blank, send it
			// Send empty packet anyways if we need to keep up with the heartbeat
			Transcriptions results = controller.getTranscriptions();
			
			if(!results.isEmpty())
			{
				String raw = results.getRawString();
				
				// Show end instead of front
				String text = (raw.length() > MAX_TRANSCRIPTION_LENGTH) ? "... " + raw.substring(raw.length() - MAX_TRANSCRIPTION_LENGTH) : raw;
				
				CensorCraft.LOGGER.info("Sending \"{}\"", raw);
				lastWordPacket = System.currentTimeMillis();
				CensorCraft.channel.send(new WordPacket(raw), PacketDistributor.SERVER.noArg());
				
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
			if(ClientConfig.SHOW_TRANSCRIPTION.get())
			{
				if(transcription != null && !transcription.toString().isBlank())
				{
					component.append(Component.literal(transcription.toString() + "\n").withColor(0xFFFFFF));
				}
			}
			
			if(ClientConfig.DEBUG.get())
			{
				component.append(Component.literal(String.format("%.1f", controller.getTimeBehind() / 1000f) + "s behind\n").withColor(0xAAAAAA));
				component.append(Component.literal("Latency: " + ClientConfig.LATENCY.get() + "\n"));
				component.append(Component.literal("Last transcribed " + recordings + " recording" + (recordings != 1 ? "s" : "") + "\n")).withColor(0xAAAAAA);
				component.append(Component.literal(controller.getTranscriptionBacklog() + " samples queued\n"));
				component.append(Component.literal("Using " + model.getFileName() + " model\n"));
			}
			// else
			// {
			// setGUIText(Component.empty());
			// }
			
			setGUIText(component, ClientConfig.DEBUG.get());
		}
		// If we're NOT supposed to be running
		else
		{
			if(controller.isInitializing())
			{
				setGUIText(Component.literal("Initializing..."));
			}
			// This is currently useless
			else if(controller.isShuttingDown())
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
	
	// private static String appendResult(String word)
	// {
	// // Separate words with spaces
	// String string = word + " ";
	//
	// if(string.length() >= transcription.capacity())
	// {
	// transcription.setLength(0);
	// transcription.append(string.substring(string.length() - transcription.capacity()));
	// }
	// else
	// {
	// int overflow = transcription.length() + string.length() - transcription.capacity();
	//
	// if(overflow > 0)
	// {
	// transcription.delete(0, overflow);
	// }
	//
	// transcription.append(string);
	// }
	//
	// return transcription.toString();
	// }
	
	private static void setGUIText(MutableComponent component, boolean forceRefresh)
	{
		GUI_TEXT = component;
		FORCE_GUI_REFRESH = forceRefresh;
	}
	
	private static void setGUIText(MutableComponent component)
	{
		setGUIText(component, false);
	}
	
	/**
	 * Restarts JScribe. The model needs to exist!
	 * 
	 * @param model name of model in model dir
	 */
	public static void setup(Path model, boolean monitorVoice, long audioContextLength)
	{
		// stopJScribe();
		ClientCensorCraft.model = model;
		ClientCensorCraft.monitorVoice = monitorVoice;
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
