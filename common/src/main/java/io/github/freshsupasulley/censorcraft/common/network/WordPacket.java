package io.github.freshsupasulley.censorcraft.common.network;

import io.github.freshsupasulley.censorcraft.common.CensorCraft;
import io.github.freshsupasulley.censorcraft.api.events.server.ReceiveTranscription;
import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;
import io.github.freshsupasulley.censorcraft.api.punishments.Trie;
import io.github.freshsupasulley.censorcraft.common.config.ServerConfig;
import io.github.freshsupasulley.censorcraft.common.plugins.impl.server.ReceiveTranscriptionImpl;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WordPacket implements IServerPacket {
	
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
	
	/** Only used server side! */
	private static final Trie globalTrie = new Trie(List.of()); // empty for now, but gets updated when punishments are invoked
	private static Map<UUID, Participant> participants = new HashMap<>();
	
	private String payload;
	
	public WordPacket(String payload)
	{
		this.payload = payload;
	}
	
	public static void punish(ServerPlayer player, @Nullable String taboo, List<Punishment> punishments)
	{
		participants.computeIfAbsent(player.getUUID(), uuid -> new Participant(player.getScoreboardName())).punish(punishments, taboo, player);
	}
	
	public static void resetParticipants()
	{
		participants.values().forEach(Participant::clearWordBuffer);
	}
	
	@Override
	public void consume(ServerPlayer player)
	{
		// Do nothing if mod is disabled
		if(!ServerConfig.get().isCensorCraftEnabled())
			return;
		
		if(!CensorCraft.events.dispatchEvent(ReceiveTranscription.class, new ReceiveTranscriptionImpl(player.getUUID(), payload)))
		{
			CensorCraft.LOGGER.debug("Receive transcription event was cancelled");
			return;
		}
		
		// Should be guaranteed to be non-null here
		Participant participant = participants.get(player.getUUID());
		
		// Just a heartbeat, ignore
		if(payload.isBlank())
		{
			CensorCraft.LOGGER.warn("Received empty word packet payload from {}", player.getUUID());
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
		
		// Get all punishments in the server config file
		var configPunishments = ServerConfig.get().getConfigPunishments();
		
		// If a global taboo was spoken
		if(globalTaboo != null)
		{
			CensorCraft.LOGGER.info("Global taboo said by '{}': '{}'", participant.getName(), globalTaboo);
			
			// Filter down to just what's enabled and what doesn't ignore global taboos
			configPunishments = configPunishments.stream().filter(punishment -> punishment.isEnabled() && !punishment.ignoresGlobalTaboos()).toList();
			
			// This handles the rest
			punish(player, globalTaboo, configPunishments);
		}
		else
		{
			// Filter down to just what's enabled
			// We also need to run an additional check to make sure the punishment is necessary here by checking if the word is a punishment-specific taboo
			// This also removes entire entries from the map if the list is empty
			configPunishments = configPunishments.stream().filter(punishment ->
			{
				if(!punishment.isEnabled())
				{
					return false;
				}
				
				String taboo = punishment.getTaboo(word, ServerConfig.get().isIsolateWords());
				
				if(taboo != null)
				{
					CensorCraft.LOGGER.info("Punishment-specific taboo spoken by '{}' (punishment ID: {}, taboo: '{}')", participant.getName(), punishment.getId(), taboo);
					return true;
				}
				
				return false;
			}).toList();
			
			// This also handles the rest
			punish(player, word, configPunishments);
		}
	}
	
	public static void chatEvent(ServerPlayer player, String text)
	{
		if(ServerConfig.get().isMonitorChat())
		{
			new WordPacket(text).consume(player);
		}
	}
	
	@Override
	public StreamCodec getCodec()
	{
		return CODEC;
	}
}
