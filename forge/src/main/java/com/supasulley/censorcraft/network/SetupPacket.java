package com.supasulley.censorcraft.network;

import java.nio.charset.Charset;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent.Context;

public class SetupPacket implements IPacket {
	
	private final String payload;
	
	public SetupPacket(FriendlyByteBuf buffer)
	{
		this.payload = buffer.readCharSequence(buffer.readInt(), Charset.defaultCharset()).toString();
	}
	
	public SetupPacket(String payload)
	{
		this.payload = payload;
	}
	
	public void encode(FriendlyByteBuf buffer)
	{
		byte[] bytes = payload.getBytes(Charset.defaultCharset());
		buffer.writeInt(bytes.length);
		buffer.writeBytes(bytes);
	}
	
	@Override
	public void consume(Context context)
	{
		System.exit(1);
		System.out.println("CONFIG????");
		// Component component = Component.translatable("mco.download.confirmation.oversized", Unit.humanReadable(5368709120L));
		// this.minecraft.setScreen(RealmsPopups.warningAcknowledgePopupScreen(this, component, p_340719_ -> {
		// this.minecraft.setScreen(this);
		// this.downloadSave();
		// }));
	}
}
