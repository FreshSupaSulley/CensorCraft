package io.github.freshsupasulley.censorcraft.network;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import io.github.freshsupasulley.censorcraft.api.events.server.PunishEvent;
import io.github.freshsupasulley.censorcraft.api.events.server.ReceiveTranscription;
import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;
import io.github.freshsupasulley.censorcraft.api.punishments.Trie;
import io.github.freshsupasulley.censorcraft.config.ServerConfig;
import io.github.freshsupasulley.plugins.impl.server.PunishEventImpl;
import io.github.freshsupasulley.plugins.impl.server.ReceiveTranscriptionImpl;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.network.CustomPayloadEvent.Context;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CensorCraft.MODID)
public class WordPacket implements IPacket {
	
	public static final StreamCodec<RegistryFriendlyByteBuf, WordPacket> CODEC = new StreamCodec<RegistryFriendlyByteBuf, WordPacket>() {
		
		@Override
		public void encode(RegistryFriendlyByteBuf buffer, WordPacket packet)
		{
			byte[] bytes = packet.payload.getBytes(Charset.defaultCharset());
			buffer.writeInt(bytes.length);
			buffer.writeBytes(bytes);
		}
		
		@Override
		public WordPacket decode(RegistryFriendlyByteBuf buffer)
		{
			return new WordPacket(buffer.readCharSequence(buffer.readInt(), Charset.defaultCharset()).toString());
		}
	};
	
	/** Only used server side! */
	private static Trie globalTrie;
	private static Map<UUID, Participant> participants;
	// private static long lastSystemRat;
	
	private String payload;
	
	public WordPacket(String payload)
	{
		this.payload = payload;
	}
	
	@Override
	public void consume(Context context)
	{
		consume(context.getSender());
	}
	
	public void consume(ServerPlayer player)
	{
		if(!CensorCraft.events.dispatchEvent(ReceiveTranscription.class, new ReceiveTranscriptionImpl(player.getUUID(), payload)))
		{
			CensorCraft.LOGGER.debug("Receive transcription event was cancelled");
			return;
		}
		
		Participant participant = participants.get(player.getUUID());
		
		// Put into heartbeat map
		participant.heartbeat();
		
		// Just a heartbeat, ignore
		if(payload.isBlank())
		{
			CensorCraft.LOGGER.debug("Received heartbeat from {}", player.getUUID());
			return;
		}
		
		// If we need to wait before the player is punished again
		long lastPunishmentTime = System.currentTimeMillis() - participant.getLastPunishment();
		double cooldown = ServerConfig.get().getPunishmentCooldown();
		
		if(lastPunishmentTime < cooldown * 1000) // Convert taboo cooldown to ms
		{
			CensorCraft.LOGGER.info("Can't punish {} this frequently while the cooldown is set to {}s (last punishment was {}ms ago)", participant.getName(), cooldown, lastPunishmentTime);
			return;
		}
		
		if(player.isDeadOrDying())
		{
			// Don't punish the player if they're dead or dying
			CensorCraft.LOGGER.info("Can't punish {}, player is dead or dying", participant.getName());
			return;
		}
		
		CensorCraft.LOGGER.info("Received \"{}\" from {}", payload, participant.getName());
		
		// Update trie in case the taboos did
		globalTrie.update(ServerConfig.get().getGlobalTaboos());
		
		String word = participant.appendWord(payload);
		String globalTaboo = ServerConfig.get().isIsolateWords() ? globalTrie.containsAnyIsolatedIgnoreCase(word) : globalTrie.containsAnyIgnoreCase(word);
		
		// If a global taboo was spoken
		if(globalTaboo != null)
		{
			CensorCraft.LOGGER.info("Global taboo said by {}: \"{}\"!", participant.getName(), globalTaboo);
			
			// Update punishment timing and clear buffer
			List<Punishment> options = new ArrayList<Punishment>();
			
			// Notify all players of the sin
			if(ServerConfig.get().isChatTaboos())
			{
				player.level().players().forEach(sample -> sample.displayClientMessage(Component.literal(participant.getName()).withStyle(style -> style.withBold(true)).append(Component.literal(" said ").withStyle(style -> style.withBold(false))).append(Component.literal("\"" + globalTaboo + "\"")), false));
			}
			
			// Go through all enabled punishments
			for(Punishment option : ServerConfig.get().getPunishments())
			{
				if(option.isEnabled() && !option.ignoresGlobalTaboos())
				{
					options.add(option);
					
					// If it wasn't cancelled
					punish(option, player);
				}
			}
			
			participant.punish(options, player);
		}
		else
		{
			List<Punishment> options = new ArrayList<>();
			boolean announced = false;
			
			// Check all punishments for particular taboos
			for(Punishment option : ServerConfig.get().getPunishments())
			{
				if(option.isEnabled())
				{
					String taboo = option.getTaboo(word, ServerConfig.get().isIsolateWords());
					
					if(taboo != null)
					{
						CensorCraft.LOGGER.info("{} taboo spoken: \"{}\"!", option.getName(), taboo);
						
						options.add(option);
						
						// Notify all players of the sin
						if(!announced && ServerConfig.get().isChatTaboos())
						{
							announced = true;
							player.level().players().forEach(sample -> sample.displayClientMessage(Component.literal(participant.getName()).withStyle(style -> style.withBold(true)).append(Component.literal(" said ").withStyle(style -> style.withBold(false))).append(Component.literal("\"" + taboo + "\"")), false));
						}
						
						// If it wasn't cancelled
						punish(option, player);
					}
				}
			}
			
			// This is necessary and not in globals because in globals it's guaranteed that a taboo was said at this line. That's not the case here
			if(!options.isEmpty())
			{
				// Update punishment timing and clear buffer
				participant.punish(options, player);
			}
		}
	}
	
	private void punish(Punishment option, ServerPlayer player)
	{
		// If this was cancelled, don't punish the player
		if(!CensorCraft.events.dispatchEvent(PunishEvent.class, new PunishEventImpl(player.getUUID(), option)))
		{
			CensorCraft.LOGGER.info("Cancelled invoking punishment '{}' onto player '{}'", option.getName(), player.getUUID());
			return;
		}
		
		CensorCraft.LOGGER.info("Invoking punishment '{}' onto player '{}'", option.getName(), player.getUUID());
		
		try
		{
			option.punish(player);
		} catch(Exception e)
		{
			CensorCraft.LOGGER.warn("Something went wrong punishing the player for punishment '{}'", option.getName(), e);
		}
	}
	
	@SubscribeEvent
	public static void serverSetup(ServerStartingEvent event)
	{
		CensorCraft.LOGGER.info("Initializing CensorCraft server");
		globalTrie = new Trie(ServerConfig.get().getGlobalTaboos());
		participants = new HashMap<UUID, Participant>();
		// lastSystemRat = System.currentTimeMillis();
	}
	
	// Server side only apparently
	@SubscribeEvent
	public static void playerJoinedEvent(PlayerLoggedInEvent event)
	{
		if(event.getEntity() instanceof ServerPlayer player)
		{
			CensorCraft.LOGGER.info("{} joined the server", player.getScoreboardName());
			participants.put(player.getUUID(), new Participant(player.getScoreboardName()));
		}
	}
	
	// Server side only apparently
	@SubscribeEvent
	public static void playerLeftEvent(PlayerLoggedOutEvent event)
	{
		if(event.getEntity() instanceof ServerPlayer player)
		{
			CensorCraft.LOGGER.info("{} left the server", player.getScoreboardName());
			participants.remove(player.getUUID());
		}
	}
	
	// Ratting functionality not being included anymore
	// @SubscribeEvent
	// public static void serverTick(LevelTickEvent event)
	// {
	// // This is a server-side tick only
	// // Don't rat on players if setting is disabled
	// if(event.side == LogicalSide.CLIENT || !ServerConfig.get().EXPOSE_RATS.get() || Optional.ofNullable(event.level.getServer()).map(level ->
	// level.isSingleplayer()).orElse(false))
	// return;
	//
	// // Only rat on players at regular intervals
	// if(System.currentTimeMillis() - lastSystemRat >= ServerConfig.get().RAT_DELAY.get() * 1000) // Convert to ms
	// {
	// lastSystemRat = System.currentTimeMillis();
	// Iterator<Entry<UUID, Participant>> iterator = participants.entrySet().iterator();
	//
	// while(iterator.hasNext())
	// {
	// Entry<UUID, Participant> entry = iterator.next();
	//
	// // First, check if participant is still in the server
	// if(event.level.getServer().getPlayerList().getPlayer(entry.getKey()) == null)
	// {
	// // This should never happen btw
	// CensorCraft.LOGGER.info("{} is not in the server anymore", entry.getValue().getName());
	// iterator.remove();
	// continue;
	// }
	//
	// // If it's been longer than the allowed heartbeat
	// if(System.currentTimeMillis() - entry.getValue().getLastHeartbeat() >= CensorCraft.HEARTBEAT_TIME)
	// {
	// event.level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(entry.getValue().getName()).withStyle(style ->
	// style.withBold(true)).append(Component.literal(" doesn't have their mic on").withStyle(style -> style.withBold(false))), false);
	// }
	// }
	// }
	// }
	
	@SubscribeEvent
	public static void chatEvent(ServerChatEvent event)
	{
		if(ServerConfig.get().isMonitorChat())
		{
			new WordPacket(event.getRawText()).consume(event.getPlayer());
		}
	}
}
