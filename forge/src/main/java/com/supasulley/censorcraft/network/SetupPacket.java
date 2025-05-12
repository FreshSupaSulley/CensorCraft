package com.supasulley.censorcraft.network;

import java.nio.charset.Charset;

import com.supasulley.censorcraft.CensorCraft;
import com.supasulley.censorcraft.ClientCensorCraft;
import com.supasulley.censorcraft.Config;
import com.supasulley.censorcraft.gui.DownloadScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
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
	
	private static boolean disconnectFlag;
	private static String disconnectModel;
	
	public SetupPacket(String model)
	{
		this.model = model;
	}
	
	public SetupPacket(FriendlyByteBuf buffer)
	{
		this.model = buffer.readCharSequence(buffer.readInt(), Charset.defaultCharset()).toString();
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
		CensorCraft.channel.send(new SetupPacket(Config.Server.PREFERRED_MODEL.get()), event.getConnection());
	}
	
	@SubscribeEvent
	public static void screenEvent(ScreenEvent.Opening event)
	{
		// I don't need to do this instanceof check but it makes me feel better
		if(event.getNewScreen() instanceof DisconnectedScreen && disconnectFlag)
		{
			disconnectFlag = false;
			
			event.setNewScreen(new PopupScreen.Builder(event.getCurrentScreen(), Component.literal("Missing model")).setMessage(Component.literal("This server requires a more advanced transcription model to play (").append(Component.literal(disconnectModel).withStyle(Style.EMPTY.withBold(true))).append(")\n\nDownload the model?")).addButton(CommonComponents.GUI_YES, (screen) ->
			{
				Minecraft.getInstance().setScreen(new DownloadScreen(disconnectModel));
			}).addButton(CommonComponents.GUI_NO, (screen) -> Minecraft.getInstance().setScreen(null)).build());
		}
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
			disconnectModel = model;
			context.getConnection().disconnect(Component.empty()); // they will never see this
		}
		else
		{
			CensorCraft.LOGGER.info("Server requested {} model (installed)", model);
			ClientCensorCraft.restart(model);
		}
	}
}
