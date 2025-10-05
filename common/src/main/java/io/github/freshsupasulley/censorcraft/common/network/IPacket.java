package io.github.freshsupasulley.censorcraft.common.network;

import net.minecraft.network.codec.StreamCodec;

public interface IPacket {
	
	StreamCodec getCodec();
}
