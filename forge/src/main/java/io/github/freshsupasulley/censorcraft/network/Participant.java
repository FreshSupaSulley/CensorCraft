package io.github.freshsupasulley.censorcraft.network;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import io.github.freshsupasulley.censorcraft.api.events.server.ServerPunishEvent;
import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;
import io.github.freshsupasulley.censorcraft.config.ServerConfig;
import io.github.freshsupasulley.plugins.impl.server.ServerPunishEventImpl;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Participant {
	
	private String name;
	private long lastPunishment; // intentionally setting lastPunishment to nothing, so the cooldown doesn't apply on start
	
	// Hold 200 characters
	private static final int BUFFER_SIZE = 200;
	private StringBuilder buffer = new StringBuilder(BUFFER_SIZE);
	
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
	
	public void punish(Map<String, List<Punishment>> configPunishments, String taboo, ServerPlayer player)
	{
		lastPunishment = System.currentTimeMillis();
		
		// Announce the punishment
		if(ServerConfig.get().isChatTaboos())
		{
			player.level().players().forEach(sample -> sample.displayClientMessage(Component.literal(name).withStyle(style -> style.withBold(true)).append(Component.literal(" said ").withStyle(style -> style.withBold(false))).append(Component.literal("\"" + taboo + "\"")), false));
		}
		
		// Trigger the server side punishments. Client side ones comes after
		configPunishments.values().stream().flatMap(List::stream).forEach((punishment -> runServerPunishment(punishment, player)));
		
		// Notify the player that they were punished
		// This will trigger the client-side punishment code (if implemented) once received
		CensorCraft.LOGGER.debug("Sending punished packet");
		
		// Send the packet
		CensorCraft.channel.send(new PunishedPacket(configPunishments), PacketDistributor.PLAYER.with(player));
		
		// Reset the participant's word buffer
		clearWordBuffer();
	}
	
	private void runServerPunishment(Punishment option, ServerPlayer player)
	{
		// If this was cancelled, don't punish the player
		if(!CensorCraft.events.dispatchEvent(ServerPunishEvent.class, new ServerPunishEventImpl(player.getUUID(), option)))
		{
			CensorCraft.LOGGER.info("Server-side punishment was cancelled");
			return;
		}
		
		CensorCraft.LOGGER.info("Invoking punishment '{}' onto player '{}'", option.getId(), player.getUUID());
		
		try
		{
			option.punish(player);
		} catch(Exception e)
		{
			CensorCraft.LOGGER.warn("Something went wrong punishing the player for punishment '{}'", option.getId(), e);
		}
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
