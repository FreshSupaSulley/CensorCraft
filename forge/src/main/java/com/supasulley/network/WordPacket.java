package com.supasulley.network;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.supasulley.censorcraft.CensorCraft;
import com.supasulley.censorcraft.Config;
import com.supasulley.censorcraft.Trie;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
	private static Map<ServerPlayer, Long> participants;
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
		participants = new HashMap<ServerPlayer, Long>();
		lastSystemRat = System.currentTimeMillis();
	}
	
	@SubscribeEvent
	public static void playerJoinedEvent(PlayerLoggedInEvent event)
	{
		if(event.getEntity() instanceof ServerPlayer player)
		{
			CensorCraft.LOGGER.info("{} joined the server", player.getName().getString());
			participants.put(player, System.currentTimeMillis());
		}
	}
	
	@SubscribeEvent
	public static void playerLeftEvent(PlayerLoggedOutEvent event)
	{
		if(event.getEntity() instanceof ServerPlayer player)
		{
			CensorCraft.LOGGER.info("{} left the server", player.getName().getString());
			participants.remove(player);
		}
	}
	
	@SubscribeEvent
	public static void serverTick(LevelTickEvent event)
	{
		// This is a server-side tick only
		// Don't rat on players if setting is disabled
		if(event.side == LogicalSide.CLIENT || !Config.Server.EXPOSE_PLAYERS.get())
			return;
		
		if(System.currentTimeMillis() - lastSystemRat >= CensorCraft.HEARTBEAT_TIME)
		{
			Iterator<Entry<ServerPlayer, Long>> iterator = participants.entrySet().iterator();
			
			while(iterator.hasNext())
			{
				Entry<ServerPlayer, Long> entry = iterator.next();
				
				// First, check if participant is still in the server
				if(event.level.getServer().getPlayerList().getPlayer(entry.getKey().getUUID()) == null)
				{
					CensorCraft.LOGGER.info("{} is not in the server anymore", entry.getKey().getName().getString());
					iterator.remove();
					continue;
				}
				
				// If it's been longer than the allowed heartbeat
				if(System.currentTimeMillis() - entry.getValue() >= CensorCraft.HEARTBEAT_TIME)
				{
					event.level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(entry.getKey().getName().getString()).withStyle(style -> style.withBold(true)).append(Component.literal(" is not participating").withStyle(style -> style.withBold(false))), false);
				}
			}
			
			lastSystemRat = System.currentTimeMillis();
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
		String payload = packet.payload;
		
		// Put into heartbeat map
		participants.put(player, System.currentTimeMillis());
		
		if(payload.isBlank())
		{
			CensorCraft.LOGGER.info("Received heartbeat from {}", player.getUUID());
			return;
		}
		
		CensorCraft.LOGGER.info("Received \"{}\" from {} ({})", payload, player.getName().getString(), player.getUUID());
		
		String taboo = tabooTree.containsAnyIgnoreCase(payload);
		if(taboo == null)
			return;
		
		CensorCraft.LOGGER.info("Taboo said by {}: \"{}\"!", player.getName().getString(), taboo);
		
		// Notify all players of the sin
		player.level().players().forEach(sample -> sample.displayClientMessage(Component.literal(player.getName().getString()).withColor(16711680).append(" said ").withColor(16777215).append("\"" + taboo + "\""), false));
		
		// Kill the player
		if(!player.isDeadOrDying())
		{
			// can the player survive the explosion (besides totems)?
			// can i get rid of new ExplosionDamageCalculator()?
			player.level().explode(null, player.level().damageSources().generic(), new ExplosionDamageCalculator(), player.getX(), player.getY(), player.getZ(), Config.Server.EXPLOSION_RADIUS.get(), Config.Server.EXPLOSION_FIRE.get(), Config.Server.EXPLOSION_GRIEFING.get() ? ExplosionInteraction.BLOCK : ExplosionInteraction.NONE);
		}
	}
}
