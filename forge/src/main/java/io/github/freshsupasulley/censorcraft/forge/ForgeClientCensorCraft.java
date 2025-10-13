package io.github.freshsupasulley.censorcraft.forge;

import io.github.freshsupasulley.censorcraft.ClientCensorCraft;
import io.github.freshsupasulley.censorcraft.forge.config.ForgeClientConfig;
import io.github.freshsupasulley.censorcraft.gui.ConfigScreen;
import io.github.freshsupasulley.censorcraft.gui.DownloadScreen;
import io.github.freshsupasulley.censorcraft.jscribe.JScribe;
import io.github.freshsupasulley.censorcraft.jscribe.Model;
import io.github.freshsupasulley.censorcraft.plugins.impl.client.CensorCraftClientAPIImpl;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

// dist has to be client here, otherwise dedicated servers will try to load the ConfigScreen class and shit the bed
// I thought omitting Bus.BOTH would be fine here because it's supposed to be the default but ig not
@Mod.EventBusSubscriber(modid = ForgeCensorCraft.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.BOTH)
public class ForgeClientCensorCraft extends ClientCensorCraft {
	
	static
	{
		INSTANCE = new ForgeClientCensorCraft();
	}
	
	private static JScribe controller;
	private static Path model;
	
	public ForgeClientCensorCraft()
	{
		super(FMLPaths.CONFIGDIR.get().resolve("censorcraft/models"));
	}
	
	// Forge bus
	@SubscribeEvent
	public static void clientSetup(FMLClientSetupEvent event)
	{
		MinecraftForge.registerConfigScreen((minecraft, screen) -> new ConfigScreen(minecraft));
		var CLIENT = new ForgeClientConfig();
		CensorCraftClientAPIImpl.INSTANCE = new CensorCraftClientAPIImpl(CLIENT.config);
	}
	
	@SubscribeEvent
	public static void screenEvent(ScreenEvent.Opening event)
	{
		// I don't need to do this instanceof check but it makes me feel better
		if(event.getNewScreen() instanceof DisconnectedScreen && disconnectFlag)
		{
			disconnectFlag = false;
			
			try
			{
				// Probably ok that this happens in the main thread
				Model model = JScribe.getModelInfo(requestedModel);
				
				if(model == null)
				{
					event.setNewScreen(errorScreen("Server requested a model that doesn't exist (" + requestedModel + ")", "Ask the server owner to fix the config"));
				}
				else
				{
					event.setNewScreen(new PopupScreen.Builder(new TitleScreen(), Component.literal("Missing model")).setMessage(Component.literal("This server requires a transcription model to play (").append(Component.literal(requestedModel + ", " + model.getSizeFancy()).withStyle(Style.EMPTY.withBold(true))).append(")\n\nDownload the model?")).addButton(CommonComponents.GUI_YES, (screen) ->
					{
						Minecraft.getInstance().setScreen(new DownloadScreen(model));
					}).addButton(CommonComponents.GUI_NO, PopupScreen::onClose).addButton(Component.literal("Learn more"), (screen) ->
					{
						Util.getPlatform().openUri(URI.create("https://www.curseforge.com/minecraft/mc-mods/censorcraft"));
						// screen.onClose();
					}).build());
				}
			} catch(IOException e)
			{
				event.setNewScreen(errorScreen("Failed to get model info", e));
			}
		}
	}
	
	// its expected that SetupPacket will be consumed before this
	@SubscribeEvent
	public static void onJoinWorld(ClientPlayerNetworkEvent.LoggingIn event)
	{
		ClientCensorCraft.onJoinWorld();
	}
	
	@SubscribeEvent
	public static void onLeaveWorld(ClientPlayerNetworkEvent.LoggingOut event)
	{
		ClientCensorCraft.onLeaveWorld();
	}
	
	@SubscribeEvent
	public static void onLevelTick(LevelTickEvent event)
	{
		// is this needed anymore now that we set the dist = client?
		if(event.side != LogicalSide.CLIENT)
			return;
		
		ClientCensorCraft.onClientTick();
	}
}
