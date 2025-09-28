package io.github.freshsupasulley.censorcraft.network;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import io.github.freshsupasulley.censorcraft.CensorCraft;
import io.github.freshsupasulley.censorcraft.ClientCensorCraft;
import io.github.freshsupasulley.censorcraft.api.events.client.ClientPunishEvent;
import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;
import io.github.freshsupasulley.plugins.impl.client.ClientPunishEventImpl;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.event.network.CustomPayloadEvent.Context;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sent to the client when they were punished.
 *
 * <p>Informs the client to reset their audio buffer, and what client-side punishments to run.</p>
 */
public class PunishedPacket implements IPacket {
	
	public static final StreamCodec<RegistryFriendlyByteBuf, PunishedPacket> CODEC = new StreamCodec<>() {
		
		@Override
		public void encode(RegistryFriendlyByteBuf buffer, PunishedPacket packet)
		{
			// Number of plugins this affected
			buffer.writeInt(packet.registry.size());
			
			packet.registry.forEach((id, punishments) ->
			{
				// The name of the plugin
				buffer.writeInt(id.length());
				buffer.writeCharSequence(id, Charset.defaultCharset());
				
				// Number of punishments for this
				buffer.writeInt(punishments.size());
				
				punishments.forEach(punishment ->
				{
					String toWrite = punishment.getClass().getName();
					buffer.writeInt(toWrite.length());
					buffer.writeCharSequence(toWrite, Charset.defaultCharset());
					
					// Append the data to this buffer
					buffer.writeByteArray(serializeConfig(punishment));
				});
			});
		}
		
		@Override
		public PunishedPacket decode(RegistryFriendlyByteBuf buffer)
		{
			Map<String, List<Punishment>> registry = new HashMap<>();
			final int plugins = buffer.readInt();
			
			for(int i = 0; i < plugins; i++)
			{
				// Get the plugin ID
				String pluginID = buffer.readCharSequence(buffer.readInt(), Charset.defaultCharset()).toString();
				
				// Get the number of punishments of this particular plugin
				final int punishments = buffer.readInt();
				registry.put(pluginID, new ArrayList<>());
				
				for(int j = 0; j < punishments; j++)
				{
					String punishmentName = buffer.readCharSequence(buffer.readInt(), Charset.defaultCharset()).toString();
					
					try
					{
						Punishment punishment = Punishment.newInstance((Class<? extends Punishment>) Class.forName(punishmentName));
						
						// Read the config from the wire
						ByteArrayInputStream in = new ByteArrayInputStream(buffer.readByteArray());
						CommentedConfig config = TomlFormat.instance().createParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8));
						punishment.fillConfig(config);
						
						// Add to our buffer
						registry.get(pluginID).add(punishment);
					} catch(Exception e)
					{
						// I believe this can only happen if the client doesn't have the punishment (mismatching mods problem)
						CensorCraft.LOGGER.error("Failed to deserialize punishment class '{}' for plugin '{}'", punishmentName, pluginID, e);
					}
				}
			}
			
			return new PunishedPacket(registry);
		}
	};
	
	private Map<String, List<Punishment>> registry;
	
	public PunishedPacket(Map<String, List<Punishment>> punishments)
	{
		this.registry = punishments;
	}
	
	@Override
	public void consume(Context context)
	{
		CensorCraft.LOGGER.info("Received punished packet");
		
		// Reset our audio buffer
		ClientCensorCraft.punished();
		
		// Trigger the client-side code of the punishment
		registry.values().stream().flatMap(List::stream).forEach(punishment ->
		{
			// Signal to plugins that the punishment is about to be executed client-side
			if(CensorCraft.events.dispatchEvent(ClientPunishEvent.class, new ClientPunishEventImpl(punishment)))
			{
				CensorCraft.LOGGER.debug("Invoking '{}' client-side punishment code", punishment.getId());
				
				try
				{
					punishment.punishClientSide();
				} catch(Exception e)
				{
					CensorCraft.LOGGER.error("Failed executing '{}' punishment", punishment.getId(), e);
				}
			}
		});
	}
	
	/**
	 * Serializes and returns the server's config file to be sent to the client.
	 */
	private static byte[] serializeConfig(Punishment punishment)
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		OutputStreamWriter writer = new OutputStreamWriter(out);
		TomlFormat.instance().createWriter().write(punishment.config, writer);
		
		try
		{
			writer.flush();
		} catch(IOException e)
		{
			// This should never happen so lazily wrap
			throw new RuntimeException(e);
		}
		
		return out.toByteArray();
	}
}
