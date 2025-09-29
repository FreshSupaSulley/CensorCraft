package io.github.freshsupasulley.censorcraft.network;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import io.github.freshsupasulley.censorcraft.ClientCensorCraft;
import io.github.freshsupasulley.censorcraft.api.events.client.ClientPunishEvent;
import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;
import io.github.freshsupasulley.plugins.impl.client.ClientPunishEventImpl;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.event.network.CustomPayloadEvent.Context;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
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
					try {
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						ObjectOutputStream oos = new ObjectOutputStream(baos);
						oos.writeObject(punishment);
						oos.flush();
						byte[] bytes = baos.toByteArray();
						
						// Write to the buffer if successful
						buffer.writeByteArray(bytes);
					} catch(Exception e) {
						CensorCraft.LOGGER.error("Failed to encode punishment '{}'", punishment.getId(), e);
						// Error will be detected during decoding
						buffer.writeByteArray(new byte[0]);
					}
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
						Class<? extends Punishment> clazz = (Class<? extends Punishment>) Class.forName(punishmentName);
						Punishment punishment;
						
						// Read object
						byte[] bytes = buffer.readByteArray();
						
						if(bytes.length == 0)
						{
							CensorCraft.LOGGER.warn("Received empty punishment object with name '{}'", punishmentName);
							punishment = Punishment.newInstance(clazz);
						}
						else
						{
							ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
							ObjectInputStream ois = new ObjectInputStream(bais);
							punishment = clazz.cast(ois.readObject());
						}
						
						// Add to our buffer
						registry.get(pluginID).add(punishment);
					} catch(Exception e)
					{
						// I believe this can only happen if the client doesn't have the punishment (mismatching mods problem)
						CensorCraft.LOGGER.error("Failed to decode punishment class '{}' for plugin '{}'", punishmentName, pluginID, e);
					}
				}
			}
			
			return new PunishedPacket(registry);
		}
	};
	
	private final Map<String, List<Punishment>> registry;
	
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
}
