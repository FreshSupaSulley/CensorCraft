package io.github.freshsupasulley.censorcraft.network;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import io.github.freshsupasulley.censorcraft.ClientCensorCraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent.Context;

public class PunishedPacket implements IPacket {
	
	private String[] punishments;
	
	public PunishedPacket(String... punishments)
	{
		this.punishments = punishments;
	}
	
	public PunishedPacket(FriendlyByteBuf buffer)
	{
		punishments = new String[buffer.readInt()];
		
		for(int i = 0; i < punishments.length; i++)
		{
			punishments[i] = buffer.readCharSequence(buffer.readInt(), Charset.defaultCharset()).toString();
		}
	}
	
	@Override
	public void encode(FriendlyByteBuf buffer)
	{
		buffer.writeInt(punishments.length);
		
		for(String string : punishments)
		{
			byte[] bytes = string.getBytes(Charset.defaultCharset());
			buffer.writeInt(bytes.length);
			buffer.writeBytes(bytes);
		}
	}
	
	@Override
	public void consume(Context context)
	{
		CensorCraft.LOGGER.info("Received punished packet: {}", Arrays.toString(punishments));
		
		// Client is the only one (so far) that needs to be executed client side
		// Needs to match getName() of PunishmentOption
		if(List.of(punishments).contains("crash"))
		{
//			new Crash().punish(null);
		}
		
		ClientCensorCraft.punished();
	}
}
