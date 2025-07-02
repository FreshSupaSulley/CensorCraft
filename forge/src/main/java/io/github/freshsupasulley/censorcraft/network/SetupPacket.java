package io.github.freshsupasulley.censorcraft.network;

import java.nio.charset.Charset;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import io.github.freshsupasulley.censorcraft.ClientCensorCraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.network.CustomPayloadEvent.Context;
import net.minecraftforge.event.network.GatherLoginConfigurationTasksEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CensorCraft.MODID)
public class SetupPacket implements IPacket {
	
	private final String model;
	private final boolean monitorVoice;
	private final long audioContextLength;
	
	public SetupPacket(String model, boolean monitorVoice, long audioContextLength)
	{
		this.model = model;
		this.monitorVoice = monitorVoice;
		this.audioContextLength = audioContextLength;
	}
	
	public SetupPacket(FriendlyByteBuf buffer)
	{
		this.model = buffer.readCharSequence(buffer.readInt(), Charset.defaultCharset()).toString();
		this.monitorVoice = buffer.readBoolean();
		this.audioContextLength = buffer.readLong();
	}
	
	// earliest event i was able to hook in
	@SubscribeEvent
	public static void playerJoinedEvent(GatherLoginConfigurationTasksEvent event)
	{
		// Inform the player of the preferred model
		CensorCraft.channel.send(new SetupPacket(CensorCraft.SERVER.getPreferredModel(), CensorCraft.SERVER.isMonitorVoice(), (long) (CensorCraft.SERVER.getContextLength() * 1000)), event.getConnection()); // CONTEXT_LENGTH is in seconds, convert to ms
	}
	
	public void encode(FriendlyByteBuf buffer)
	{
		byte[] bytes = model.getBytes(Charset.defaultCharset());
		buffer.writeInt(bytes.length);
		buffer.writeBytes(bytes);
		
		buffer.writeBoolean(monitorVoice);
		buffer.writeLong(audioContextLength);
	}
	
	@Override
	public void consume(Context context)
	{
		CensorCraft.LOGGER.info("Consuming setup packet (model: {}, monitorVoice: {})", model, monitorVoice);
		
		// If we don't have the model requested by the server
		if(monitorVoice && !ClientCensorCraft.hasModel(model))
		{
			CensorCraft.LOGGER.info("Client does not have {} model installed", model);
			
			ClientCensorCraft.requestModelDownload(model);
			context.getConnection().disconnect(Component.empty()); // they will never see this
		}
		else
		{
			ClientCensorCraft.setup(ClientCensorCraft.getModelPath(model), monitorVoice, audioContextLength);
		}
	}
}
