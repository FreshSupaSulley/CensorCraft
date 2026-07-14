package io.github.freshsupasulley.censorcraft.network;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import io.github.freshsupasulley.censorcraft.api.events.server.ChatTabooEvent;
import io.github.freshsupasulley.censorcraft.api.events.server.ServerPunishEvent;
import io.github.freshsupasulley.censorcraft.api.punishments.PunishmentParam;
import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;
import io.github.freshsupasulley.censorcraft.config.ServerConfig;
import io.github.freshsupasulley.censorcraft.plugins.impl.server.ChatTabooEventImpl;
import io.github.freshsupasulley.censorcraft.plugins.impl.server.ServerPunishEventImpl;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Participant {
	
	private final String name;
	private long lastPunishment; // intentionally setting lastPunishment to nothing, so the cooldown doesn't apply on start
	
	// Hold 200 characters
	private static final int BUFFER_SIZE = 200;
	private final StringBuilder buffer = new StringBuilder(BUFFER_SIZE);
	private int punishmentCount;
	
	public Participant(String name)
	{
		this.name = name;
	}
	
	/**
	 * Appends the word just said to their "last said" buffer (separated with spaces) and returns the buffer.
	 *
	 * @param word word to append
	 * @return buffer of words most recently said
	 */
	public String appendWord(String word)
	{
		// Separate words with spaces
		String string = word + " ";
		
		if(string.length() >= BUFFER_SIZE)
		{
			buffer.setLength(0);
			buffer.append(string.substring(string.length() - BUFFER_SIZE));
		}
		else
		{
			int overflow = buffer.length() + string.length() - BUFFER_SIZE;
			
			if(overflow > 0)
			{
				buffer.delete(0, overflow);
			}
			
			buffer.append(string);
		}
		
		return buffer.toString();
	}
	
	public void punish(Map<Punishment, @Nullable Set<String>> punishments, ServerPlayer player)
	{
		if(punishments.isEmpty())
		{
			CensorCraft.LOGGER.info("Punishments are empty, this will only clear the audio buffer on the client");
		}
		else
		{
			// Only count this as a punishment if there's a punishment provided
			punishmentCount++;
		}
		
		lastPunishment = System.currentTimeMillis();
		
		// Trigger the server side punishments. Client side ones comes after
		// 1. filter down to successful punishments, THEN 2. filter down to just ones with chat_taboo enabled (to ensure all of them still run regardless of this setting)
		var toChatSuccesses = punishments.entrySet().stream().filter(entry -> runServerPunishment(entry.getKey(), entry.getValue(), player)).filter(p -> ServerConfig.get().getParam(PunishmentParam.CHAT_TABOOS, p.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		
		// Announce the punishment. We are collapsing all ran punishments into one big message here
		// The only reason I'm announcing the punishment AFTER running it is for my niche plugin so it can access instance variables during ChatTabooEvent
		if(!toChatSuccesses.isEmpty())
		{
			// insane deflation
			Component taboos = toChatSuccesses.values().stream().flatMap(Set::stream).distinct().map(string -> Component.literal("\"" + string + "\"").withStyle(style -> style.withBold(true))).reduce(Component.empty(), (og, sample) -> og.append(sample).append(Component.literal(", ")));
			var parts = taboos.getSiblings();
			
			if(!parts.isEmpty())
			{
				// The last component is always just a comma
				parts.removeLast();
				
				if(parts.size() > 1)
				{
					// Remove the last 2 elements and readd the very last one after we've added ", and"
					var readd = parts.removeLast();
					parts.removeLast();
					
					parts.add(Component.literal(", and "));
					parts.add(readd);
				}
				
				// Allow plugins to change what gets sent
				var component = Component.empty().append(Component.literal(name).withStyle(style -> style.withBold(true))).append(Component.literal(" said ")).append(taboos);
				var event = new ChatTabooEventImpl(punishments.keySet(), player.getUUID(), component);
				
				if(CensorCraft.events.dispatchEvent(ChatTabooEvent.class, event))
				{
					player.level().players().forEach(sample -> sample.displayClientMessage((Component) event.getText(), false));
				}
			}
		}
		
		// Notify the player that they were punished
		// This will trigger the client-side punishment code (if implemented) once received
		CensorCraft.LOGGER.debug("Sending punished packet");
		
		// Send the packet
		CensorCraft.INSTANCE.sendToPlayer(new PunishedPacket(punishments.values().stream().filter(Objects::nonNull).flatMap(Set::stream).collect(Collectors.toSet()), punishments.keySet()), player);
		
		// Reset the participant's word buffer
		clearWordBuffer();
	}
	
	private boolean runServerPunishment(Punishment punishment, Set<String> taboos, ServerPlayer player)
	{
		// Check if we need to punish everyone. If so, only punish alive players
		List<ServerPlayer> players;
		
		if(!ServerConfig.get().getParam(PunishmentParam.PUNISH_EVERYONE, punishment))
		{
			players = List.of(player);
		}
		else
		{
			players = Objects.requireNonNull(player.getServer()).getPlayerList().getPlayers().stream().filter(sample -> !sample.isDeadOrDying()).toList();
			CensorCraft.LOGGER.info("Everyone ({}) is getting punished", players.size());
		}
		
		boolean singleSuccess = false;
		
		for(var victim : players)
		{
			// If this was cancelled, don't punish the player
			if(!CensorCraft.events.dispatchEvent(ServerPunishEvent.class, new ServerPunishEventImpl(player.getUUID(), punishment)))
			{
				CensorCraft.LOGGER.info("Server-side punishment was cancelled");
				continue;
			}
			
			CensorCraft.LOGGER.info("Invoking punishment '{}' onto player '{}'", punishment.getId(), victim.getUUID());
			
			try
			{
				// Pop any punishment job into the queue
				var job = punishment.punish(victim, taboos);
				
				if(job != null)
				{
					CensorCraft.PUNISHMENT_JOBS.add(job);
				}
				
				singleSuccess = true;
			} catch(Exception e)
			{
				CensorCraft.LOGGER.warn("Something went wrong punishing the player for punishment '{}'", punishment.getId(), e);
			}
		}
		
		return singleSuccess;
	}
	
	public int getPunishmentCount()
	{
		return punishmentCount;
	}
	
	public void clearWordBuffer()
	{
		buffer.setLength(0);
	}
	
	public String getName()
	{
		return name;
	}
	
	public long getLastPunishment()
	{
		return lastPunishment;
	}
}
