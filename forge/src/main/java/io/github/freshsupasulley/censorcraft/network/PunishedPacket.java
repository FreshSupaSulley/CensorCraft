package io.github.freshsupasulley.censorcraft.network;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import io.github.freshsupasulley.censorcraft.ClientCensorCraft;
import io.github.freshsupasulley.censorcraft.api.events.client.ClientAcknowledgePunish;
import io.github.freshsupasulley.censorcraft.config.punishments.Crash;
import io.github.freshsupasulley.plugins.impl.client.ClientAcknowledgePunishImpl;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent.Context;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

public class PunishedPacket implements IPacket {
	
	public static final StreamCodec<RegistryFriendlyByteBuf, PunishedPacket> CODEC = new StreamCodec<RegistryFriendlyByteBuf, PunishedPacket>()
	{
		@Override
		public void encode(RegistryFriendlyByteBuf buffer, PunishedPacket packet)
		{
			buffer.writeInt(packet.punishments.length);
			
			for(String string : packet.punishments)
			{
				byte[] bytes = string.getBytes(Charset.defaultCharset());
				buffer.writeInt(bytes.length);
				buffer.writeBytes(bytes);
			}
		}
		
		@Override
		public PunishedPacket decode(RegistryFriendlyByteBuf buffer)
		{
			String[] punishments = new String[buffer.readInt()];
			
			for(int i = 0; i < punishments.length; i++)
			{
				punishments[i] = buffer.readCharSequence(buffer.readInt(), Charset.defaultCharset()).toString();
			}
			
			return new PunishedPacket(punishments);
		}
	};
	
	private String[] punishments;
	
	public PunishedPacket(String... punishments)
	{
		this.punishments = punishments;
	}
	
	@Override
	public void consume(Context context)
	{
		CensorCraft.LOGGER.info("Received punished packet: {}", Arrays.toString(punishments));
		
		// Notify any APIs that we're now in the client-side
		CensorCraft.events.dispatchEvent(ClientAcknowledgePunish.class, new ClientAcknowledgePunishImpl(punishments));
		
		// Client is the only one (so far) that needs to be executed client side
		// Needs to match getName() of PunishmentOption
		if(List.of(punishments).contains("crash"))
		{
			new Crash().punish((ServerPlayer) null);
		}
		
		ClientCensorCraft.punished();
	}
}
