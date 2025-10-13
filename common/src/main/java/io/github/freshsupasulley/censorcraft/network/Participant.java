package io.github.freshsupasulley.censorcraft.network;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import io.github.freshsupasulley.censorcraft.api.events.server.ChatTabooEvent;
import io.github.freshsupasulley.censorcraft.api.events.server.ServerPunishEvent;
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
	
	public void punish(Map<Punishment, @Nullable String> punishments, ServerPlayer player)
	{
		if(punishments.isEmpty())
		{
			CensorCraft.LOGGER.warn("Punishments are empty, this will only clear the audio buffer on the client!");
		}
		else
		{
			// Only count this as a punishment if there's a punishment provided
			punishmentCount++;
		}
		
		lastPunishment = System.currentTimeMillis();
		
		// Trigger the server side punishments. Client side ones comes after
		var successes = punishments.entrySet().stream().filter(entry -> runServerPunishment(entry.getKey(), player)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		
		// Announce the punishment
		// The only reason I'm announcing the punishment AFTER running it is for my niche plugin so it can access instance variables during ChatTabooEvent
		if(ServerConfig.get().isChatTaboos())
		{
			Component taboos = successes.values().stream().distinct().map(string -> Component.literal("\"" + string + "\"").withStyle(style -> style.withBold(true))).reduce(Component.empty(), (og, sample) -> og.append(sample).append(Component.literal(", ")));
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
		CensorCraft.INSTANCE.sendToPlayer(new PunishedPacket(punishments.keySet()), player);
		
		// Reset the participant's word buffer
		clearWordBuffer();
	}
	
	private boolean runServerPunishment(Punishment option, ServerPlayer player)
	{
		// If this was cancelled, don't punish the player
		if(!CensorCraft.events.dispatchEvent(ServerPunishEvent.class, new ServerPunishEventImpl(player.getUUID(), option)))
		{
			CensorCraft.LOGGER.info("Server-side punishment was cancelled");
			return false;
		}
		
		CensorCraft.LOGGER.info("Invoking punishment '{}' onto player '{}'", option.getId(), player.getUUID());
		
		try
		{
			option.punish(player);
			return true;
		} catch(Exception e)
		{
			CensorCraft.LOGGER.warn("Something went wrong punishing the player for punishment '{}'", option.getId(), e);
		}
		
		return false;
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
