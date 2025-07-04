package io.github.freshsupasulley.censorcraft;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import io.github.freshsupasulley.censorcraft.network.PunishedPacket;
import io.github.freshsupasulley.censorcraft.network.SetupPacket;
import io.github.freshsupasulley.censorcraft.network.WordPacket;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;

@Mod(CensorCraft.MODID)
public class CensorCraft {
	
	public static final String MODID = "censorcraft";
	public static final Logger LOGGER = LogUtils.getLogger();
	
	// Packets
	public static final long HEARTBEAT_TIME = 30000, HEARTBEAT_SAFETY_NET = 5000;
	public static SimpleChannel channel;
	
	public CensorCraft(FMLJavaModLoadingContext context)
	{
		var modBusGroup = context.getModBusGroup();
		FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::commonSetup);
		// FMLClientSetupEvent.getBus(modBusGroup).addListener(this::clientSetup);
	}
	
	// Packet communication setup
	private void commonSetup(FMLCommonSetupEvent event)
	{
		// ig just bump this with each new major (and thus incompatible with the last) update?
		final int protocolVersion = 2;
		
		event.enqueueWork(() ->
		{
			channel = ChannelBuilder.named(CensorCraft.MODID).networkProtocolVersion(protocolVersion).simpleChannel();
			
			channel.configuration().clientbound().addMain(SetupPacket.class, SetupPacket.CODEC, SetupPacket::consume);
			channel.play().serverbound().addMain(WordPacket.class, WordPacket.CODEC, WordPacket::consume);
			channel.play().clientbound().addMain(PunishedPacket.class, PunishedPacket.CODEC, PunishedPacket::consume);
			
			channel.build();
		});
	}
}
