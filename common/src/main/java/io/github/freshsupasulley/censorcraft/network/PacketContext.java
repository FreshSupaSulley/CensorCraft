package io.github.freshsupasulley.censorcraft.network;

import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public interface PacketContext {
	
	@Nullable ServerPlayer getSender();
	void disconnect();
}
