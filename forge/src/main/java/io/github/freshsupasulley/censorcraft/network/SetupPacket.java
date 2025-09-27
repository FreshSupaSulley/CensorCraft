package io.github.freshsupasulley.censorcraft.network;

import java.nio.charset.Charset;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import io.github.freshsupasulley.censorcraft.ClientCensorCraft;
import io.github.freshsupasulley.censorcraft.config.ServerConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.event.network.CustomPayloadEvent.Context;
import net.minecraftforge.event.network.GatherLoginConfigurationTasksEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CensorCraft.MODID)
public class SetupPacket implements IPacket {
	//
	// public static final StreamCodec<FriendlyByteBuf, SetupPacket> CODEC = new StreamCodec<FriendlyByteBuf, SetupPacket>()
	// {
	// @Override
	// public SetupPacket decode(FriendlyByteBuf buffer)
	// {
	// return decode(buffer);
	// }
	//
	// @Override
	// public void encode(FriendlyByteBuf buffer, SetupPacket packet)
	// {
	// encode(buffer, packet);
	// }
	// };
	//
	// public static final StreamCodec<ByteBuf, BlockPos> STREAM_CODEC = new StreamCodec<ByteBuf, BlockPos>() {
	// public BlockPos decode(ByteBuf p_335731_) {
	// return FriendlyByteBuf.readBlockPos(p_335731_);
	// }
	//
	// public void encode(ByteBuf p_329093_, BlockPos p_330029_) {
	// FriendlyByteBuf.writeBlockPos(p_329093_, p_330029_);
	// }
	// };
	
	public static final StreamCodec<FriendlyByteBuf, SetupPacket> CODEC = new StreamCodec<FriendlyByteBuf, SetupPacket>()
	{
		@Override
		public void encode(FriendlyByteBuf buffer, SetupPacket packet)
		{
			byte[] bytes = packet.model.getBytes(Charset.defaultCharset());
			buffer.writeInt(bytes.length);
			buffer.writeBytes(bytes);
			buffer.writeInt(packet.audioContextLength);
		}
		
		@Override
		public SetupPacket decode(FriendlyByteBuf buffer)
		{
			var model = buffer.readCharSequence(buffer.readInt(), Charset.defaultCharset()).toString();
			var audioContextLength = buffer.readInt();
			return new SetupPacket(model, audioContextLength);
		}
	};
	
	private final String model;
	private final int audioContextLength;
	
	public SetupPacket(String model, int audioContextLength)
	{
		this.model = model;
		this.audioContextLength = audioContextLength;
	}
	
	// earliest event i was able to hook in
	@SubscribeEvent
	public static void playerJoinedEvent(GatherLoginConfigurationTasksEvent event)
	{
		// Inform the player of the preferred model
		CensorCraft.channel.send(new SetupPacket(ServerConfig.get().getPreferredModel(), (int) (ServerConfig.get().getContextLength() * 1000)), event.getConnection()); // CONTEXT_LENGTH is in seconds, convert to ms
	}
	
	@Override
	public void consume(Context context)
	{
		CensorCraft.LOGGER.info("Consuming setup packet (model: {})", model);
		
		// If we don't have the model requested by the server
		if(!ClientCensorCraft.hasModel(model))
		{
			CensorCraft.LOGGER.info("Client does not have {} model installed", model);
			
			ClientCensorCraft.requestModelDownload(model);
			context.getConnection().disconnect(Component.empty()); // they will never see this
		}
		else
		{
			ClientCensorCraft.setup(ClientCensorCraft.getModelPath(model), audioContextLength);
		}
	}
}
