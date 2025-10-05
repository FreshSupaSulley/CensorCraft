package io.github.freshsupasulley.censorcraft.common.network;

import net.minecraft.server.level.ServerPlayer;

public interface IServerPacket extends IPacket {
	
	void consume(ServerPlayer player);
}
