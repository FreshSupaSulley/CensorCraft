package io.github.freshsupasulley.censorcraft.forge;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.logging.LogUtils;
import io.github.freshsupasulley.censorcraft.api.CensorCraftPlugin;
import io.github.freshsupasulley.censorcraft.api.ForgeCensorCraftPlugin;
import io.github.freshsupasulley.censorcraft.api.events.server.ServerConfigEvent;
import io.github.freshsupasulley.censorcraft.CensorCraft;
import io.github.freshsupasulley.censorcraft.config.ServerConfig;
import io.github.freshsupasulley.censorcraft.network.*;
import io.github.freshsupasulley.censorcraft.plugins.impl.server.CensorCraftServerAPIImpl;
import io.github.freshsupasulley.censorcraft.plugins.impl.server.ServerConfigEventImpl;
import io.github.freshsupasulley.censorcraft.forge.config.ForgeServerConfig;
import io.github.freshsupasulley.censorcraft.forge.network.PacketContextImpl;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.event.network.GatherLoginConfigurationTasksEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Only register common and server events here.
 */
@Mod(ForgeCensorCraft.MODID)
@Mod.EventBusSubscriber(modid = CensorCraft.MODID)
public class ForgeCensorCraft extends CensorCraft {
	
	public static final String MODID = "censorcraft";
	public static final Logger LOGGER = LogUtils.getLogger();
	
	// Packets
	public static SimpleChannel channel;
	
	public ForgeCensorCraft(FMLJavaModLoadingContext loadingContext)
	{
		var modBusGroup = loadingContext.getModBusGroup();
		
		// Packet communication setup
		FMLCommonSetupEvent.getBus(modBusGroup).addListener((event) ->
		{
			// ig just bump this with each new major (and thus incompatible with the last) update?
			final int protocolVersion = 3;
			
			event.enqueueWork(() ->
			{
				channel = ChannelBuilder.named(ForgeCensorCraft.MODID).networkProtocolVersion(protocolVersion).simpleChannel();
				
				channel.configuration().clientbound().addMain(SetupPacket.class, SetupPacket.CODEC, this::safeConsume);
				channel.play().serverbound().addMain(WordPacket.class, WordPacket.CODEC, this::safeConsume);
				channel.play().clientbound().addMain(PunishedPacket.class, PunishedPacket.CODEC, (packet, context) -> packet.consume(new PacketContextImpl(context)));
				
				channel.build();
			});
		});
	}
	
	private void safeConsume(IPacket packet, CustomPayloadEvent.Context context)
	{
		try
		{
			packet.consume(new PacketContextImpl(context));
		} catch(Exception e)
		{
			CensorCraft.LOGGER.error("Something went wrong consuming packet", e);
		}
	}
	
	// Allow server admins to track punishments
	@SubscribeEvent
	public static void serverTickEvent(TickEvent.ServerTickEvent.Post event)
	{
		Scoreboard scoreboard = event.getServer().getScoreboard();
		
		// Only use the objective if the server admin wants one
		var objective = scoreboard.getObjective(MODID);
		
		if(objective == null)
		{
			return;
		}
		
		// We got the green light, fill the scoreboard
		var participants = WordPacket.participants;
		
		// Add each participant to the scoreboard
		for(Map.Entry<UUID, Participant> sample : participants.entrySet())
		{
			var player = event.getServer().getPlayerList().getPlayer(sample.getKey());
			
			// If the player left forget about it
			if(player == null) continue;
			
			scoreboard.getOrCreatePlayerScore(player, objective).set(sample.getValue().getPunishmentCount());//.display(Component.literal(player.getScoreboardName()));
		}
	}
	
	@SubscribeEvent
	public static void chatEvent(ServerChatEvent event)
	{
		WordPacket.chatEvent(event.getPlayer(), event.getRawText());
	}
	
	@SubscribeEvent
	public static void playerJoinedEvent(GatherLoginConfigurationTasksEvent event)
	{
		// Not using overridden methods here because there is no serverplayer yet
		channel.send(new SetupPacket(ServerConfig.get().getPreferredModel(), (int) (ServerConfig.get().getContextLength() * 1000)), event.getConnection()); // CONTEXT_LENGTH is in seconds, convert to ms
	}
	
	@SubscribeEvent
	private static void serverSetup(ServerAboutToStartEvent event)
	{
		var SERVER = new ForgeServerConfig(event.getServer());
		// Now the plugins can see the API impl
		CensorCraftServerAPIImpl.INSTANCE = new CensorCraftServerAPIImpl(SERVER.config);
		CensorCraft.events.dispatchEvent(ServerConfigEvent.class, new ServerConfigEventImpl());
	}
	
	@SubscribeEvent
	private static void registerCommands(RegisterCommandsEvent event)
	{
		// probably highest op level for this command? (4)
		event.getDispatcher().register(Commands.literal("censorcraft").requires(source -> source.hasPermission(4)).then(Commands.literal("enable").executes(ctx -> setEnabled(ctx.getSource(), true))).then(Commands.literal("disable").executes(ctx -> setEnabled(ctx.getSource(), false))));
	}
	
	private static int setEnabled(CommandSourceStack source, boolean enabled) throws CommandSyntaxException
	{
		boolean state = ServerConfig.get().isCensorCraftEnabled();
		
		if(state == enabled)
		{
			throw new SimpleCommandExceptionType(Component.literal("CensorCraft is already " + (state ? "enabled" : "disabled"))).create();
		}
		
		// If we just enabled it
		if(enabled)
		{
			// Clear the accumulated participants buffer
			WordPacket.resetParticipants();
			
			for(ServerPlayer player : source.getServer().getPlayerList().getPlayers())
			{
				// Signal to clients to reset their audio buffer
				// (so if they spoke a taboo right as its enabled, they don't get punished)
				// This could be paired with temporarily ignoring taboos for like 1s server side if required
				ForgeCensorCraft.channel.send(new PunishedPacket(List.of()), PacketDistributor.PLAYER.with(player));
			}
		}
		
		// Let's also notify them that the mod is active
		source.getLevel().players().forEach(sample -> sample.displayClientMessage(Component.literal("CensorCraft is now ").append(Component.literal(enabled ? "enabled" : "disabled").withStyle(style -> style.withBold(true))), false));
		
		ForgeCensorCraft.LOGGER.info("Setting CensorCraft enabled state: {}", enabled);
		ServerConfig.get().config.set("enable_censorcraft", enabled);
		return 1;
	}
	
	@Override
	protected List<CensorCraftPlugin> findPlugins()
	{
		List<CensorCraftPlugin> plugins = new ArrayList<>();
		
		ModList.get().getAllScanData().forEach(scan ->
		{
			scan.getAnnotations().forEach(annotationData ->
			{
				if(annotationData.annotationType().getClassName().equals(ForgeCensorCraftPlugin.class.getName()))
				{
					try
					{
						Class<?> clazz = Class.forName(annotationData.memberName());
						
						if(CensorCraftPlugin.class.isAssignableFrom(clazz))
						{
							plugins.add((CensorCraftPlugin) clazz.getDeclaredConstructor().newInstance());
						}
					} catch(Exception e)
					{
						ForgeCensorCraft.LOGGER.warn("Failed to load Forge plugin '{}'", annotationData.memberName(), e);
					}
				}
			});
		});
		
		return plugins;
	}
	
	@Override
	public void sendToPlayer(IPacket punishedPacket, ServerPlayer player)
	{
		channel.send(punishedPacket, PacketDistributor.PLAYER.with(player));
	}
	
	@Override
	public void sendToServer(IPacket wordPacket)
	{
		channel.send(wordPacket, PacketDistributor.SERVER.noArg());
	}
}
