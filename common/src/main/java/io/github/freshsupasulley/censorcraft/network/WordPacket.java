package io.github.freshsupasulley.censorcraft.network;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import io.github.freshsupasulley.censorcraft.api.events.server.ReceiveTranscription;
import io.github.freshsupasulley.censorcraft.api.punishments.PunishmentParam;
import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;
import io.github.freshsupasulley.censorcraft.api.punishments.Trie;
import io.github.freshsupasulley.censorcraft.config.ServerConfig;
import io.github.freshsupasulley.censorcraft.plugins.impl.server.ReceiveTranscriptionImpl;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

import java.nio.charset.Charset;
import java.util.*;

public class WordPacket implements IPacket {
	
	public static final StreamCodec<RegistryFriendlyByteBuf, WordPacket> CODEC = new StreamCodec<>() {
		
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
	
	public static final Map<UUID, Participant> participants = new HashMap<>();
	private final String payload;
	
	public WordPacket(String payload)
	{
		this.payload = payload;
	}
	
	public static void punish(ServerPlayer player, Map<Punishment, Set<String>> punishments)
	{
		participants.computeIfAbsent(player.getUUID(), uuid -> new Participant(player.getScoreboardName())).punish(punishments, player);
	}
	
	public static void resetParticipants()
	{
		participants.values().forEach(Participant::clearWordBuffer);
	}
	
	private void innerConsume(ServerPlayer player, boolean fromChat)
	{
		// Do nothing if mod is disabled
		if(!ServerConfig.get().isCensorCraftEnabled())
			return;
		
		if(!CensorCraft.events.dispatchEvent(ReceiveTranscription.class, new ReceiveTranscriptionImpl(player.getUUID(), payload)))
		{
			CensorCraft.LOGGER.debug("Receive transcription event was cancelled");
			return;
		}
		
		// Put the participant in the map if new
		Participant participant = participants.computeIfAbsent(player.getUUID(), uuid -> new Participant(player.getScoreboardName()));
		
		// Just a heartbeat, ignore
		if(payload.isBlank())
		{
			CensorCraft.LOGGER.warn("Received empty word packet payload from {}", player.getUUID());
			return;
		}
		
		// This check happens twice btw, here and in participant's punish method. We don't want to append the word to the rolling buffer if they're dead
		if(player.isDeadOrDying())
		{
			// Don't punish the player if they're dead or dying
			CensorCraft.LOGGER.info("Can't punish {}, player is not alive", participant.getName());
			return;
		}
		
		CensorCraft.LOGGER.info("Received \"{}\" from {}", payload, participant.getName());
		
		String word = participant.appendWord(payload);
		
		// Get all punishments in the server config file
		var configPunishments = ServerConfig.get().getConfigPunishments();
		Map<Punishment, Set<String>> toFire = new HashMap<>();
		
		// Check each punishment's individual taboo option. Filter down to just what's enabled too
		// We also need to run an additional check to make sure the punishment is necessary here by checking if the word is a punishment-specific taboo
		// This also removes entire entries from the map if the list is empty
		for(var punishment : configPunishments)
		{
			// Only handle through enabled punishments
			if(!punishment.isEnabled())
				continue;
			
			// If it's from chat and we're not monitoring it, abandon
			if(fromChat && !ServerConfig.get().getParam(PunishmentParam.MONITOR_CHAT, punishment))
				continue;
			
			// If we need to wait before the player is punished again
			long lastPunishmentTime = System.currentTimeMillis() - participant.getLastPunishment();
			double cooldown = ServerConfig.get().getParam(PunishmentParam.PUNISHMENT_COOLDOWN, punishment).doubleValue();
			
			if(lastPunishmentTime < cooldown * 1000) // Convert taboo cooldown to ms
			{
				CensorCraft.LOGGER.info("Can't punish {} this frequently (punishment: {}) while the cooldown is at {}s (last punishment was {}ms ago)", participant.getName(), punishment.getId(), cooldown, lastPunishmentTime);
				continue;
			}
			
			// Grab taboos
			final boolean isolateWords = ServerConfig.get().getParam(PunishmentParam.ISOLATE_WORDS, punishment);
			Trie globalTrie = new Trie(ServerConfig.get().getParam(PunishmentParam.TABOOS, punishment));
			Set<String> taboos = isolateWords ? globalTrie.findIsolatedEntries(word) : globalTrie.findEntries(word);
			
			// If taboos were spoken
			if(taboos != null)
			{
				CensorCraft.LOGGER.info("Taboo(s) said by '{}' (punishment: {}, taboo(s): {})", participant.getName(), punishment.getId(), taboos);
				toFire.put(punishment, taboos);
			}
		}
		
		// Only punish if we actually got a punishment to run
		if(!toFire.isEmpty())
		{
			// Assemble the toFire to an actual map
			punish(player, toFire);
		}
	}
	
	@Override
	public void consume(PacketContext context)
	{
		innerConsume(context.getSender(), false);
	}
	
	public static void chatEvent(ServerPlayer player, String text)
	{
		new WordPacket(text).innerConsume(player, true);
	}
}
