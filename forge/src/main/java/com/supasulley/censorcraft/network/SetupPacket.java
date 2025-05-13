package com.supasulley.censorcraft.network;

import java.io.IOException;
import java.nio.charset.Charset;

import com.supasulley.censorcraft.CensorCraft;
import com.supasulley.censorcraft.ClientCensorCraft;
import com.supasulley.censorcraft.Config;
import com.supasulley.censorcraft.gui.DownloadScreen;

import io.github.freshsupasulley.JScribe;
import io.github.freshsupasulley.Model;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.network.CustomPayloadEvent.Context;
import net.minecraftforge.event.network.GatherLoginConfigurationTasksEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CensorCraft.MODID)
public class SetupPacket implements IPacket {
	
	private final String model;
	private final long audioContextLength;
	
	private static boolean disconnectFlag;
	private static String requestedModel;
	
	public SetupPacket(String model, long audioContextLength)
	{
		this.model = model;
		this.audioContextLength = audioContextLength;
	}
	
	public SetupPacket(FriendlyByteBuf buffer)
	{
		this.model = buffer.readCharSequence(buffer.readInt(), Charset.defaultCharset()).toString();
		this.audioContextLength = buffer.readLong();
	}
	
	// @SubscribeEvent
	// public static void playerJoinedEvent(PlayerLoggedInEvent event)
	// {
	// // Inform the player of the preferred model
	// CensorCraft.channel.send(new SetupPacket(Config.Server.PREFERRED_MODEL.get()), ((ServerPlayer) event.getEntity()).connection.getConnection());
	// }
	
	// @SubscribeEvent
	// public static void playerJoinedEvent(ConnectionStartEvent event)
	// {
	// if(event.isClient()) return;
	//
	// System.out.println("SENDING TO CLIENT");
	// // Inform the player of the preferred model
	// CensorCraft.channel.send(new SetupPacket(Config.Server.PREFERRED_MODEL.get()), event.getConnection());
	// }
	
	// earliest event i was able to hook in
	@SubscribeEvent
	public static void playerJoinedEvent(GatherLoginConfigurationTasksEvent event)
	{
		// Inform the player of the preferred model
		CensorCraft.channel.send(new SetupPacket(Config.Server.PREFERRED_MODEL.get(), (long) (Config.Server.CONTEXT_LENGTH.get() * 1000)), event.getConnection()); // CONTEXT_LENGTH is in seconds, convert to ms
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
					event.setNewScreen(errorScreen("Server requested a model that doesn't exist (" + requestedModel + ")", "Ask the server owner to fix censorcraft-client.toml"));
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
	
	public void encode(FriendlyByteBuf buffer)
	{
		byte[] bytes = model.getBytes(Charset.defaultCharset());
		buffer.writeInt(bytes.length);
		buffer.writeBytes(bytes);
	}
	
	@Override
	public void consume(Context context)
	{
		// If we don't have the model requested by the server
		if(!ClientCensorCraft.hasModel(model))
		{
			CensorCraft.LOGGER.info("Client does not have {} model installed", model);
			
			disconnectFlag = true;
			requestedModel = model;
			context.getConnection().disconnect(Component.empty()); // they will never see this
		}
		else
		{
			CensorCraft.LOGGER.info("Server requested {} model (already installed)", model);
			ClientCensorCraft.setup(ClientCensorCraft.getModelPath(model), audioContextLength);
		}
	}
}
