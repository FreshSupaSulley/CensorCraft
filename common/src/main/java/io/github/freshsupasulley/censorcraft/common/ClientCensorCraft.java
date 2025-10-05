package io.github.freshsupasulley.censorcraft.common;

import de.maxhenkel.voicechat.api.events.ClientSoundEvent;
import io.github.freshsupasulley.censorcraft.api.events.client.SendTranscriptionEvent;
import io.github.freshsupasulley.censorcraft.common.config.ClientConfig;
import io.github.freshsupasulley.censorcraft.common.jscribe.JScribe;
import io.github.freshsupasulley.censorcraft.common.jscribe.RollingAudioBuffer;
import io.github.freshsupasulley.censorcraft.common.jscribe.Transcriptions;
import io.github.freshsupasulley.censorcraft.common.network.WordPacket;
import io.github.freshsupasulley.censorcraft.common.plugins.impl.client.SendTranscriptionImpl;
import io.github.freshsupasulley.whisperjni.LibraryUtils;
import io.github.freshsupasulley.whisperjni.WhisperFullParams;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public abstract class ClientCensorCraft {
	
	private static JScribe controller;
	private static Path model;
	
	protected static boolean loggedIn, startJScribeAttempt;
	
	// Setup
	protected static boolean disconnectFlag;
	protected static String requestedModel;
	
	// GUI
	public static boolean NEEDS_RESTART;
	public static final int PADDING = 5;
	private static final int MAX_TRANSCRIPTION_LENGTH = 60;
	
	public static MutableComponent GUI_TEXT, transcription;
	public static boolean FORCE_GUI_REFRESH;
	
	// private static StringBuilder transcription = new StringBuilder(MAX_TRANSCRIPTION_LENGTH);
	// private static String transcription;
	private static int recordings;
	
	/** MS between samples before the rolling audio buffer is cleared */
	private static final long DRAIN_DELAY = 1000, MIN_SAMPLE_MS = 200;
	
	private static final int SAMPLE_RATE = 48000;
	private static RollingAudioBuffer ringBuffer;
	private static long lastSample, lastTranscription;
	
	private static Path vadModel;
	private static long audioContextLength;
	
	public static ClientCensorCraft INSTANCE;
	
	static
	{
		try
		{
			vadModel = Files.createTempFile("vadModel", ".bin");
			LibraryUtils.exportVADModel(CensorCraft.LOGGER, vadModel);
		} catch(IOException e)
		{
			CensorCraft.LOGGER.error("Failed to extract VAD model", e);
		}
	}
	
	public ClientCensorCraft()
	{
		INSTANCE = this;
	}
	
	public static void onClientSound(ClientSoundEvent event)
	{
		lastSample = System.currentTimeMillis();
		
		// Wait until we're ready
		if(ringBuffer != null)
		{
			ringBuffer.append(event.getRawAudio());
		}
	}
	
	private static int msToSamples(long ms)
	{
		return (int) ((ms * SAMPLE_RATE) / 1000);
	}
	
	public abstract Path getModelDir();
	
	public Path getModelPath(String modelName)
	{
		return getModelDir().resolve(modelName + ".bin");
	}
	
	public boolean hasModel(String modelName)
	{
		return getModelPath(modelName).toFile().exists();
	}
	
	protected static void startJScribe()
	{
		// From the mod config screen
		NEEDS_RESTART = false;
		
		if(controller != null && controller.isInUse())
		{
			CensorCraft.LOGGER.debug("Ignoring start request, JScribe is already running");
			return;
		}
		
		// This lets error messages (if any) appear again
		setGUIText(Component.empty());
		
		JScribe.Builder builder = new JScribe.Builder(model);
		builder.setLogger(CensorCraft.LOGGER);
		// Build params with VAD properties of client
		WhisperFullParams params = JScribe.createWhisperFullParams();
		params.vad = ClientConfig.get().getVAD();
		params.vad_model_path = vadModel.toAbsolutePath().toString();
		params.vadParams.threshold = ClientConfig.get().getVADThreshold();
		params.vadParams.min_speech_duration_ms = ClientConfig.get().getVADMinSpeechDurationMS();
		params.vadParams.min_silence_duration_ms = ClientConfig.get().getVADMinSilenceDurationMS();
		params.vadParams.max_speech_duration_s = ClientConfig.get().getVADMaxSpeechDurationS();
		params.vadParams.speech_pad_ms = ClientConfig.get().getVADSpeechPadMS();
		params.vadParams.samples_overlap = ClientConfig.get().getVADSamplesOverlap();
		builder.setWhisperFullParams(params);
		// builder.warmUpModel();
		
		if(ClientConfig.get().isUseVulkan())
		{
			CensorCraft.LOGGER.warn("Vulkan enabled in client config");
			builder.useVulkan();
		}
		
		controller = builder.build();
		
		// Reset debug params
		transcription = null;
		ringBuffer.drain();
		recordings = 0;
		
		// Model might have changed, might as well reinstantiate
		try
		{
			controller.start();
		} catch(Exception e)
		{
			CensorCraft.LOGGER.error("Failed to start JScribe", e);
		}
	}
	
	protected static void stopJScribe()
	{
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
	protected void onJoinWorld()
	{
		CensorCraft.LOGGER.info("LoggingIn event fired");
		loggedIn = true;
		startJScribe();
	}
	
	protected void onLeaveWorld()
	{
		CensorCraft.LOGGER.info("LoggingOut event fired");
		loggedIn = false;
		stopJScribe();
	}
	
	/**
	 * Every (client) tick, JScribe should be running. If it's not, we need to signal that to the user.
	 */
	protected static void onClientTick()
	{
		// no clue if .player can be null but im compensating for it anyways
		boolean playerAlive = Optional.ofNullable(Minecraft.getInstance().player).map(LocalPlayer::isAlive).orElse(false);
		
		// If player is dead, don't run JScribe
		if(!playerAlive && controller.isRunning())
		{
			stopJScribe();
		}
		
		// If the mic source changed, user restarted it, etc.
		if(NEEDS_RESTART)
		{
			// setGUIText(Component.literal("Restarting...").withStyle(style -> style.withBold(true)));
			stopJScribe();
			startJScribe();
		}
		
		// If we're supposed to be recording
		if(loggedIn && playerAlive && !controller.isInitializing())
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
					controller.transcribe(ringBuffer);
				}
				
				ringBuffer.drain();
			}
			else
			{
				// How many samples we need for our target buffer size
				long latency = ClientConfig.get().getLatency();
				
				// If we’ve collected enough and latency says we want another sample
				if(ringBuffer.getSize() >= msToSamples(MIN_SAMPLE_MS) && System.currentTimeMillis() - lastTranscription >= latency)
				{
					lastTranscription = System.currentTimeMillis();
					controller.transcribe(ringBuffer);
				}
			}
			
			MutableComponent component = Component.empty();
			
			// Beyond this point, we need it to be running and actively transcribing
			// If it's not blank, send it
			Transcriptions results = controller.getTranscriptions();
			
			if(!results.isEmpty())
			{
				// Collect transcriptions
				StringBuffer processBuffer = new StringBuffer();
				
				results.getTranscriptions().forEach(t -> processBuffer.append(t.text()));
				
				// Configure what to send
				String raw = processBuffer.toString();
				
				if(CensorCraft.events.dispatchEvent(SendTranscriptionEvent.class, new SendTranscriptionImpl(raw)))
				{
					CensorCraft.LOGGER.info("Sending \"{}\"", raw);
					CensorCraft.INSTANCE.sendToServer(new WordPacket(raw));
				}
				
				// Update GUI
				// Prepend with ...
				if(raw.length() > MAX_TRANSCRIPTION_LENGTH)
				{
					raw = "... " + raw.substring(raw.length() - MAX_TRANSCRIPTION_LENGTH);
				}
				
				transcription = Component.literal(raw).withColor(0xFFFFFF);
				
				recordings = results.getTotalRecordings();
			}
			
			// 15000 warning
			if(controller.getTimeBehind() > 15000)
			{
				component.append(Component.literal("CensorCraft is far behind\n").withStyle(style -> style.withBold(true).withColor(0xFF0000)).append(Component.literal("Consider raising transcription latency\n").withStyle(style -> style.withBold(false).withColor(0xAAAAAA))));
			}
			
			if(ClientConfig.get().isShowTranscription() && transcription != null)
			{
				component.append(transcription).append("\n");
			}
			
			if(ClientConfig.get().isDebug())
			{
				component.append(Component.literal(String.format("%.1f", controller.getTimeBehind() / 1000f) + "s behind\n").withColor(0xAAAAAA));
				component.append(Component.literal("Latency: " + ClientConfig.get().getLatency() + "\n"));
				component.append(Component.literal("Last transcribed " + recordings + " recording" + (recordings != 1 ? "s" : "") + "\n")).withColor(0xAAAAAA);
				component.append(Component.literal(controller.getTranscriptionBacklog() + " samples queued\n"));
				component.append(Component.literal("Using " + model.getFileName() + " model\n"));
			}
			// else
			// {
			// setGUIText(Component.empty());
			// }
			
			setGUIText(component, ClientConfig.get().isDebug());
		}
		// If we're NOT supposed to be running
		else
		{
			if(controller.isInitializing())
			{
				// Always indicate we're initializing
				setGUIText(Component.literal("Starting CensorCraft...").withStyle(style -> style.withBold(true)), true);
			}
			// This is currently useless
			else if(controller.isShuttingDown())
			{
				setGUIText(Component.literal("Stopping CensorCraft...").withStyle(style -> style.withBold(true)), true);
			}
			else if(!controller.isInUse())
			{
				setGUIText(Component.literal("Stopped CensorCraft").withStyle(style -> style.withBold(true)));
			}
		}
	}
	
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
	public static void setup(Path model, int audioContextLength)
	{
		// stopJScribe();
		ClientCensorCraft.model = model;
		ringBuffer = new RollingAudioBuffer(audioContextLength, SAMPLE_RATE); // hold the amount of MS set by the server
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
			ringBuffer.drain();
			controller.reset();
		}
	}
}
