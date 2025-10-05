package io.github.freshsupasulley.censorcraft.common.network;

public interface IClientPacket extends IPacket {
	
	void consume(PacketContext context);
}
