package com.supasulley.censorcraft.network;

import java.nio.charset.Charset;

import com.supasulley.censorcraft.CensorCraft;
import com.supasulley.censorcraft.ClientCensorCraft;
import com.supasulley.censorcraft.config.Config;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.network.CustomPayloadEvent.Context;
import net.minecraftforge.event.network.GatherLoginConfigurationTasksEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CensorCraft.MODID)
public class SetupPacket implements IPacket {
	
	private final String model;
	private final long audioContextLength;
	
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
	
	public void encode(FriendlyByteBuf buffer)
	{
		byte[] bytes = model.getBytes(Charset.defaultCharset());
		buffer.writeInt(bytes.length);
		buffer.writeBytes(bytes);
		
		buffer.writeLong(audioContextLength);
	}
	
	@Override
	public void consume(Context context)
	{
		// If we don't have the model requested by the server
		if(!ClientCensorCraft.hasModel(model))
		{
			CensorCraft.LOGGER.info("Client does not have {} model installed", model);
			
			ClientCensorCraft.requestModelDownload(model);
			context.getConnection().disconnect(Component.empty()); // they will never see this
		}
		else
		{
			CensorCraft.LOGGER.info("Server requested {} model (already installed)", model);
			ClientCensorCraft.setup(ClientCensorCraft.getModelPath(model), audioContextLength);
		}
	}
}
