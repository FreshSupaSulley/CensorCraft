package io.github.freshsupasulley.censorcraft.forge.network;

import io.github.freshsupasulley.censorcraft.common.network.PacketContext;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class PacketContextImpl implements PacketContext {
	
	private CustomPayloadEvent.Context context;
	
	public PacketContextImpl(CustomPayloadEvent.Context context)
	{
		this.context = context;
	}
	
	@Override
	public void disconnect()
	{
		context.getConnection().disconnect(Component.empty());
	}
}
