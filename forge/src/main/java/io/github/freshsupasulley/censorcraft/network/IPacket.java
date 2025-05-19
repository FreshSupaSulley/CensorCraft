package io.github.freshsupasulley.censorcraft.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

public interface IPacket {
	
	void encode(FriendlyByteBuf buffer);
	void consume(CustomPayloadEvent.Context context);
}
