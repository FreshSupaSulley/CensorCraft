package io.github.freshsupasulley.censorcraft.network;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import io.github.freshsupasulley.censorcraft.config.punishments.PunishmentOption;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CensorCraft.MODID)
public class WordPacket implements IPacket {
	
	/** Only used server side! */
	private static Trie globalTrie;
	private static Map<UUID, Participant> participants;
//	private static long lastSystemRat;
	
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
		
		if(lastPunishmentTime < CensorCraft.SERVER.getPunishmentCooldown() * 1000) // Convert taboo cooldown to ms
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
		globalTrie.update(CensorCraft.SERVER.getGlobalTaboos());
		
		String word = participant.appendWord(payload);
		String globalTaboo = CensorCraft.SERVER.isIsolateWords() ? globalTrie.containsAnyIsolatedIgnoreCase(word) : globalTrie.containsAnyIgnoreCase(word);
		
		boolean announced = false;
		
		// If a global taboo was spoken
		if(globalTaboo != null)
		{
			CensorCraft.LOGGER.info("Global taboo said by {}: \"{}\"!", participant.getName(), globalTaboo);
			
			// Update punishment timing and clear buffer
			List<PunishmentOption<?>> options = new ArrayList<PunishmentOption<?>>();
			
			// Notify all players of the sin
			if(CensorCraft.SERVER.isChatTaboos())
			{
				announced = true;
				player.level().players().forEach(sample -> sample.displayClientMessage(Component.literal(participant.getName()).withStyle(style -> style.withBold(true)).append(Component.literal(" said ").withStyle(style -> style.withBold(false))).append(Component.literal("\"" + globalTaboo + "\"")), false));
			}
			
			// Go through all enabled punishments
			for(PunishmentOption<?> option : CensorCraft.SERVER.getPunishments())
			{
				if(option.isEnabled() && !option.ignoresGlobalTaboos())
				{
					options.add(option);
					
					CensorCraft.LOGGER.info("Invoking {} punishment", option.getName());
					option.punish(player);
				}
			}
			
			System.out.println("TIME TO SEND SOMETHING");
			participant.punish(options, player);
		}
		else
		{
			List<PunishmentOption<?>> options = new ArrayList<PunishmentOption<?>>();
			
			// Check all punishments for particular taboos
			for(PunishmentOption<?> option : CensorCraft.SERVER.getPunishments())
			{
				if(option.isEnabled())
				{
					String taboo = option.getTaboo(word, CensorCraft.SERVER.isIsolateWords());
					
					if(taboo != null)
					{
						CensorCraft.LOGGER.info("{} taboo spoken: \"{}\"!", option.getName(), taboo);
						
						options.add(option);
						
						// Notify all players of the sin
						if(!announced && CensorCraft.SERVER.isChatTaboos())
						{
							announced = true;
							player.level().players().forEach(sample -> sample.displayClientMessage(Component.literal(participant.getName()).withStyle(style -> style.withBold(true)).append(Component.literal(" said ").withStyle(style -> style.withBold(false))).append(Component.literal("\"" + taboo + "\"")), false));
						}
						
						option.punish(player);
					}
				}
			}
			
			// This is necessary and not in globals because in globals it's guaranteed that a taboo was said at this line. That's not the case here
			if(!options.isEmpty())
			{
				System.out.println("TIME TO SEND SOMETHING2");
				// Update punishment timing and clear buffer
				participant.punish(options, player);
			}
		}
	}
	
	@SubscribeEvent
	public static void serverSetup(ServerStartingEvent event)
	{
		CensorCraft.LOGGER.info("Initializing CensorCraft server");
		globalTrie = new Trie(CensorCraft.SERVER.getGlobalTaboos());
		participants = new HashMap<UUID, Participant>();
//		lastSystemRat = System.currentTimeMillis();
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
//	@SubscribeEvent
//	public static void serverTick(LevelTickEvent event)
//	{
//		// This is a server-side tick only
//		// Don't rat on players if setting is disabled
//		if(event.side == LogicalSide.CLIENT || !CensorCraft.SERVER.EXPOSE_RATS.get() || Optional.ofNullable(event.level.getServer()).map(level -> level.isSingleplayer()).orElse(false))
//			return;
//		
//		// Only rat on players at regular intervals
//		if(System.currentTimeMillis() - lastSystemRat >= CensorCraft.SERVER.RAT_DELAY.get() * 1000) // Convert to ms
//		{
//			lastSystemRat = System.currentTimeMillis();
//			Iterator<Entry<UUID, Participant>> iterator = participants.entrySet().iterator();
//			
//			while(iterator.hasNext())
//			{
//				Entry<UUID, Participant> entry = iterator.next();
//				
//				// First, check if participant is still in the server
//				if(event.level.getServer().getPlayerList().getPlayer(entry.getKey()) == null)
//				{
//					// This should never happen btw
//					CensorCraft.LOGGER.info("{} is not in the server anymore", entry.getValue().getName());
//					iterator.remove();
//					continue;
//				}
//				
//				// If it's been longer than the allowed heartbeat
//				if(System.currentTimeMillis() - entry.getValue().getLastHeartbeat() >= CensorCraft.HEARTBEAT_TIME)
//				{
//					event.level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(entry.getValue().getName()).withStyle(style -> style.withBold(true)).append(Component.literal(" doesn't have their mic on").withStyle(style -> style.withBold(false))), false);
//				}
//			}
//		}
//	}
	
	@SubscribeEvent
	public static void chatEvent(ServerChatEvent event)
	{
		if(CensorCraft.SERVER.isMonitorChat())
		{
			new WordPacket(event.getRawText()).consume(event.getPlayer());
		}
	}
}
