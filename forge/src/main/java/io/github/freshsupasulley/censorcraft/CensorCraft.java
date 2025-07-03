package io.github.freshsupasulley.censorcraft;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import io.github.freshsupasulley.censorcraft.config.ClientConfig;
import io.github.freshsupasulley.censorcraft.config.ServerConfig;
import io.github.freshsupasulley.censorcraft.network.IPacket;
import io.github.freshsupasulley.censorcraft.network.PunishedPacket;
import io.github.freshsupasulley.censorcraft.network.SetupPacket;
import io.github.freshsupasulley.censorcraft.network.WordPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.SimpleChannel;
import net.minecraftforge.network.simple.SimpleConnection;

@Mod(CensorCraft.MODID)
@Mod.EventBusSubscriber(modid = CensorCraft.MODID)
public class CensorCraft {
	
	public static final String MODID = "censorcraft";
	public static final Logger LOGGER = LogUtils.getLogger();
	
	// Packets
	public static final long HEARTBEAT_TIME = 30000, HEARTBEAT_SAFETY_NET = 5000;
	public static SimpleChannel channel;
	
	public static ClientConfig CLIENT;
	public static ServerConfig SERVER;
	
	public CensorCraft(FMLJavaModLoadingContext context)
	{
		// Forbidden words are defined at the server level
		CLIENT = new ClientConfig();
		
		var modBusGroup = context.getModBusGroup();
		FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::commonSetup);
		FMLClientSetupEvent.getBus(modBusGroup).addListener(this::clientSetup);
	}
	
	// Not an FML event
	@SubscribeEvent
	public static void serverSetup(ServerAboutToStartEvent event)
	{
		SERVER = new ServerConfig(event.getServer());
	}
	
	// Mod Event Bus events
	private void commonSetup(FMLCommonSetupEvent event)
	{
		final int protocolVersion = 1;
		
		event.enqueueWork(() ->
		{
			channel = ChannelBuilder.named(CensorCraft.MODID).networkProtocolVersion(protocolVersion).simpleChannel();
			// https://github.com/nexusnode/crafting-dead/blob/1a0bb37eed5384735b75e5c961b72af436da709e/crafting-dead-immerse/src/main/java/com/craftingdead/immerse/network/NetworkChannel.java#L55
			// channel.messageBuilder(SetupPacket.class,
			// NetworkDirection.LOGIN_TO_CLIENT).encoder(SetupPacket::encode).decoder(SetupPacket::new).consumerMainThread(SetupPacket::consume).add();
			// channel.messageBuilder(SetupPacket.class, NetworkDirection.LOGIN_TO_CLIENT).encoder((packet, buffer) -> packet.encode(buffer)).decoder(buffer -> new
			// SetupPacket(buffer)).consumerMainThread((packet, context) -> packet.consume(context)).add();
			// channel.messageBuilder(WordPacket.class,
			// NetworkDirection.PLAY_TO_SERVER).encoder(WordPacket::encode).decoder(WordPacket::new).consumerMainThread(WordPacket::consume).add();
			
			channel.configuration().clientbound().add(SetupPacket.class, SetupPacket.CODEC, SetupPacket::consume);
			// register(channel, NetworkDirection.CONFIGURATION_TO_CLIENT, SetupPacket.class);
			
			channel.play().serverbound().add(WordPacket.class, WordPacket.CODEC, WordPacket::consume);
			// register(channel, NetworkDirection.PLAY_TO_SERVER, WordPacket.class);
			
			channel.play().clientbound().add(PunishedPacket.class, PunishedPacket.CODEC, PunishedPacket::consume);
			// register(channel, NetworkDirection.PLAY_TO_CLIENT, PunishedPacket.class);
			// channel.messageBuilder(WordPacket.class,
			// NetworkDirection.PLAY_TO_SERVER).encoder(WordPacket::encode).decoder(WordPacket::decode).consumerMainThread(WordPacket::consume).add();
		});
	}
	
	public void clientSetup(FMLClientSetupEvent event)
	{
		ClientCensorCraft.clientSetup(event);
	}
}
