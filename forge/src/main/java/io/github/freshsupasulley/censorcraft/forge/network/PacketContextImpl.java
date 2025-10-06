package io.github.freshsupasulley.censorcraft.forge.network;

import io.github.freshsupasulley.censorcraft.network.PacketContext;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import org.jetbrains.annotations.Nullable;

public class PacketContextImpl implements PacketContext {
	
	private CustomPayloadEvent.Context context;
	
	public PacketContextImpl(CustomPayloadEvent.Context context)
	{
		this.context = context;
	}
	
	@Override
	public @Nullable ServerPlayer getSender()
	{
		return context.getSender();
	}
	
	@Override
	public void disconnect()
	{
		context.getConnection().disconnect(Component.empty());
	}
}
