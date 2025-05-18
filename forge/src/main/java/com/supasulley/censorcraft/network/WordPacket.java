package com.supasulley.censorcraft.network;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.supasulley.censorcraft.CensorCraft;
import com.supasulley.censorcraft.config.Config;
import com.supasulley.censorcraft.config.punishments.PunishmentOption;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CensorCraft.MODID)
public class WordPacket implements IPacket {
	
	/** Only used server side! */
	private static Trie globalTrie;
	private static Map<UUID, Participant> participants;
	private static long lastSystemRat;
	
	private String payload;
	
	public WordPacket(String payload)
	{
		this.payload = payload;
	}
	
	public WordPacket(FriendlyByteBuf buffer)
	{
		this.payload = buffer.readCharSequence(buffer.readInt(), Charset.defaultCharset()).toString();
	}
	
	@Override
	public void encode(FriendlyByteBuf buffer)
	{
		byte[] bytes = payload.getBytes(Charset.defaultCharset());
		buffer.writeInt(bytes.length);
		buffer.writeBytes(bytes);
	}
	
	@Override
	public void consume(CustomPayloadEvent.Context context)
	{
		consume(context.getSender());
	}
	
	public void consume(ServerPlayer player)
	{
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
		
		if(lastPunishmentTime < Config.Server.PUNISHMENT_COOLDOWN.get() * 1000) // Convert taboo cooldown to ms
		{
			CensorCraft.LOGGER.debug("Can't punish {} this frequently (last punishment was {}ms ago)", participant.getName(), lastPunishmentTime);
			return;
		}
		
		if(player.isDeadOrDying())
		{
			// Don't punish the player if they're dead or dying
			CensorCraft.LOGGER.debug("Can't punish {}, player is dead or dying", participant.getName());
			return;
		}
		
		CensorCraft.LOGGER.info("Received \"{}\" from {}", payload, participant.getName());
		
		// Update trie in case the taboos did
		globalTrie.update(Config.Server.GLOBAL_TABOO.get());
		
		String word = participant.appendWord(payload);
		String globalTaboo = Config.Server.ISOLATE_WORDS.get() ? globalTrie.containsAnyIsolatedIgnoreCase(word) : globalTrie.containsAnyIgnoreCase(word);
		
		// If a global taboo was spoken
		if(globalTaboo != null)
		{
			CensorCraft.LOGGER.info("Global taboo said by {}: \"{}\"!", participant.getName(), globalTaboo);
			
			// Update punishment timing and clear buffer
			List<PunishmentOption> options = new ArrayList<PunishmentOption>();
			
			// Notify all players of the sin
			if(Config.Server.CHAT_TABOOS.get())
			{
				player.level().players().forEach(sample -> sample.displayClientMessage(Component.literal(participant.getName()).withStyle(style -> style.withBold(true)).append(Component.literal(" said ").withStyle(style -> style.withBold(false))).append(Component.literal("\"" + globalTaboo + "\"")), false));
			}
			
			// Go through all enabled punishments
			for(PunishmentOption option : Config.Server.PUNISHMENTS)
			{
				if(option.isEnabled() && !option.ignoresGlobalTaboos())
				{
					options.add(option);
					
					CensorCraft.LOGGER.info("Invoking {} punishment", option.getName());
					option.punish(player);
				}
			}
			
			participant.punish(options, player);
		}
		else
		{
			List<PunishmentOption> options = new ArrayList<PunishmentOption>();
			
			// Check all punishments for particular taboos
			for(PunishmentOption option : Config.Server.PUNISHMENTS)
			{
				if(option.isEnabled())
				{
					String taboo = option.getTaboo(word);
					
					if(taboo != null)
					{
						CensorCraft.LOGGER.info("{} taboo spoken: \"{}\"!", option.getName(), taboo);
						
						options.add(option);
						
						// Notify all players of the sin
						if(Config.Server.CHAT_TABOOS.get())
						{
							player.level().players().forEach(sample -> sample.displayClientMessage(Component.literal(participant.getName()).withStyle(style -> style.withBold(true)).append(Component.literal(" said ").withStyle(style -> style.withBold(false))).append(Component.literal("\"" + taboo + "\"")), false));
						}
						
						option.punish(player);
					}
				}
			}
			
			// Update punishment timing and clear buffer
			participant.punish(options, player);
		}
	}
	
	@SubscribeEvent
	public static void serverSetup(ServerStartingEvent event)
	{
		CensorCraft.LOGGER.info("Initializing CensorCraft server");
		globalTrie = new Trie(Config.Server.GLOBAL_TABOO.get());
		participants = new HashMap<UUID, Participant>();
		lastSystemRat = System.currentTimeMillis();
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
	
	@SubscribeEvent
	public static void serverTick(LevelTickEvent event)
	{
		// This is a server-side tick only
		// Don't rat on players if setting is disabled
		if(event.side == LogicalSide.CLIENT || !Config.Server.EXPOSE_RATS.get())
			return;
		
		// Only rat on players at regular intervals
		if(System.currentTimeMillis() - lastSystemRat >= Config.Server.RAT_DELAY.get() * 1000) // Convert to ms
		{
			lastSystemRat = System.currentTimeMillis();
			Iterator<Entry<UUID, Participant>> iterator = participants.entrySet().iterator();
			
			while(iterator.hasNext())
			{
				Entry<UUID, Participant> entry = iterator.next();
				
				// First, check if participant is still in the server
				if(event.level.getServer().getPlayerList().getPlayer(entry.getKey()) == null)
				{
					// This should never happen btw
					CensorCraft.LOGGER.info("{} is not in the server anymore", entry.getValue().getName());
					iterator.remove();
					continue;
				}
				
				// If it's been longer than the allowed heartbeat
				if(System.currentTimeMillis() - entry.getValue().getLastHeartbeat() >= CensorCraft.HEARTBEAT_TIME)
				{
					event.level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(entry.getValue().getName()).withStyle(style -> style.withBold(true)).append(Component.literal(" doesn't have their mic on").withStyle(style -> style.withBold(false))), false);
				}
			}
		}
	}
	
	@SubscribeEvent
	public static void chatEvent(ServerChatEvent event)
	{
		if(Config.Server.MONITOR_CHAT.get())
		{
			new WordPacket(event.getRawText()).consume(event.getPlayer());
		}
	}
}
