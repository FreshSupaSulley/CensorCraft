package com.supasulley.network;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.supasulley.censorcraft.CensorCraft;
import com.supasulley.censorcraft.Config;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CensorCraft.MODID)
public class WordPacket {
	
	/** Only used server side! */
	private static Trie tabooTree;
	private static Map<UUID, Participant> participants;
	private static long lastSystemRat;
	
	private String payload;
	
	/**
	 * Transcribed audio packet to process on the server.
	 * 
	 * @param payload transcription segment
	 */
	public WordPacket(String payload)
	{
		this.payload = payload;
	}
	
	@SubscribeEvent
	public static void serverSetup(ServerStartingEvent event)
	{
		CensorCraft.LOGGER.info("Loading taboo tree");
		tabooTree = new Trie(Config.Server.TABOO.get());
		participants = new HashMap<UUID, Participant>();
		lastSystemRat = System.currentTimeMillis();
	}
	
	// Server side only apparently
	@SubscribeEvent
	public static void playerJoinedEvent(PlayerLoggedInEvent event)
	{
		if(event.getEntity() instanceof ServerPlayer player)
		{
			CensorCraft.LOGGER.info("{} joined the server", player.getName().getString());
			participants.put(player.getUUID(), new Participant(player.getName().getString()));
		}
	}
	
	// Server side only apparently
	@SubscribeEvent
	public static void playerLeftEvent(PlayerLoggedOutEvent event)
	{
		if(event.getEntity() instanceof ServerPlayer player)
		{
			CensorCraft.LOGGER.info("{} left the server", player.getName().getString());
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
	
	public static void encode(WordPacket packet, RegistryFriendlyByteBuf buffer)
	{
		byte[] bytes = packet.payload.getBytes(Charset.defaultCharset());
		buffer.writeInt(bytes.length);
		buffer.writeBytes(bytes);
	}
	
	public static WordPacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new WordPacket(buffer.readCharSequence(buffer.readInt(), Charset.defaultCharset()).toString());
	}
	
	public static void consume(WordPacket packet, CustomPayloadEvent.Context context)
	{
		ServerPlayer player = context.getSender();
		Participant participant = participants.get(player.getUUID());
		
		// Put into heartbeat map
		participant.heartbeat();
		
		// Just a heartbeat, ignore
		if(packet.payload.isBlank())
		{
			CensorCraft.LOGGER.trace("Received heartbeat from {}", player.getUUID());
			return;
		}
		
		CensorCraft.LOGGER.info("Received \"{}\" from {}", packet.payload, player.getName().getString());
		
		String taboo = tabooTree.containsAnyIgnoreCase(participant.appendWord(packet.payload));
		if(taboo == null)
			return;
		
		// Update punishment timing and clear buffer
		participant.punish();
		
		// If we need to wait before the player is punished again
		if(System.currentTimeMillis() - participant.getLastPunishment() < Config.Server.TABOO_COOLDOWN.get() * 1000) // Convert taboo cooldown to ms
		{
			CensorCraft.LOGGER.info("Can't punish {} this frequently (last punishment at {}ms)", participant.getName(), participant.getLastPunishment());
			return;
		}
		
		CensorCraft.LOGGER.info("Taboo said by {}: \"{}\"!", player.getName().getString(), taboo);
		
		// Notify all players of the sin
		if(Config.Server.CHAT_TABOOS.get())
		{
			player.level().players().forEach(sample -> sample.displayClientMessage(Component.literal(player.getName().getString()).withStyle(style -> style.withBold(true)).append(Component.literal(" said ").withStyle(style -> style.withBold(false))).append(Component.literal("\"" + taboo + "\"")), false));
		}
		
		// Kill the player
		if(!player.isDeadOrDying())
		{
			punish(player);
		}
	}
	
	private static void punish(ServerPlayer player)
	{
		if(Config.Server.ENABLE_EXPLOSION.get())
		{
			player.level().explode(null, player.level().damageSources().generic(), new ExplosionDamageCalculator(), player.getX(), player.getY(), player.getZ(), Config.Server.EXPLOSION_RADIUS.get(), Config.Server.EXPLOSION_FIRE.get(), Config.Server.EXPLOSION_GRIEFING.get() ? ExplosionInteraction.BLOCK : ExplosionInteraction.NONE);
		}
		
		if(Config.Server.ENABLE_LIGHTNING.get())
		{
			// Number of strikes
			for(int i = 0; i < Config.Server.LIGHTNING_STRIKES.get(); i++)
			{
				LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, player.level());
				bolt.setPos(player.position());
				player.level().addFreshEntity(bolt);
			}
		}
		
		// If the player needs to die
		if(Config.Server.KILL_PLAYER.get())
		{
			// If we should ignore totems
			if(Config.Server.IGNORE_TOTEMS.get())
			{
				// Generic kill ignores totems
				player.kill(player.serverLevel());
			}
			else
			{
				// Generic will stop at totems
				player.hurtServer(player.serverLevel(), player.level().damageSources().generic(), Float.MAX_VALUE);
			}
		}
	}
}
