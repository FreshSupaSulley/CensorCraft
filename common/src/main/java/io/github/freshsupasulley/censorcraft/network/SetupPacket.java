package io.github.freshsupasulley.censorcraft.network;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import io.github.freshsupasulley.censorcraft.ClientCensorCraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.nio.charset.Charset;

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
	
	public static final StreamCodec<FriendlyByteBuf, SetupPacket> CODEC = new StreamCodec<>() {
		
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
	
	@Override
	public void consume(PacketContext context)
	{
		CensorCraft.LOGGER.info("Consuming setup packet (model: {}) {}", model, ClientCensorCraft.INSTANCE);
		
		// If we don't have the model requested by the server
		if(!ClientCensorCraft.INSTANCE.hasModel(model))
		{
			CensorCraft.LOGGER.info("Client does not have {} model installed", model);
			
			ClientCensorCraft.requestModelDownload(model);
			context.disconnect();
		}
		else
		{
			CensorCraft.LOGGER.info("Client has model installed");
			ClientCensorCraft.setup(ClientCensorCraft.INSTANCE.getModelPath(model), audioContextLength);
		}
	}
}
