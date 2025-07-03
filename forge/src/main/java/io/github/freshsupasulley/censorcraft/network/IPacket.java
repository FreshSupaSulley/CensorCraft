package io.github.freshsupasulley.censorcraft.network;

import net.minecraftforge.event.network.CustomPayloadEvent;

public interface IPacket {
	
	void consume(CustomPayloadEvent.Context context);
}
