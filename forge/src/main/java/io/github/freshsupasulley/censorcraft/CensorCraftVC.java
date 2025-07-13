package io.github.freshsupasulley.censorcraft;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.ClientSoundEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;

/**
 * VoicechatPlugins aren't sided, so instead of bundling the plugin in {@link ClientCensorCraft}, this is the alternative to prevent a non-fatal error message
 * from appearing in dedicated servers.
 */
@ForgeVoicechatPlugin
public class CensorCraftVC implements VoicechatPlugin {
	
	@Override
	public String getPluginId()
	{
		return CensorCraft.MODID;
	}
	
	@Override
	public void registerEvents(EventRegistration registration)
	{
		CensorCraft.LOGGER.info("Registering SVC events");
		registration.registerEvent(ClientSoundEvent.class, this::onClientSound);
	}
	
	public void onClientSound(ClientSoundEvent event)
	{
		ClientCensorCraft.onClientSound(event);
	}
}
