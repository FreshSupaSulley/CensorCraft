package io.github.freshsupasulley.censorcraft.common.gui;

import io.github.freshsupasulley.censorcraft.common.ClientCensorCraft;
import io.github.freshsupasulley.censorcraft.common.config.ClientConfig;
import io.github.freshsupasulley.censorcraft.common.jscribe.Transcriber;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {
	
	private final Minecraft minecraft;
	
	public ConfigScreen(Minecraft minecraft)
	{
		super(Component.literal("Configure CensorCraft"));
		
		this.minecraft = minecraft;
	}
	
	@Override
	public void onClose()
	{
		// Changes should update immediately
		ClientCensorCraft.GUI_TEXT = Component.empty();
		super.onClose();
	}
	
	/**
	 * Runs everytime the mod config menu is opened.
	 */
	@Override
	protected void init()
	{
		// Lists
		final int listY = ClientCensorCraft.PADDING * 2 + font.lineHeight;
		final int optionsWidth = this.width - ClientCensorCraft.PADDING;
		
		Button restartButton = Button.builder(Component.literal("Restart"), button -> button.active = false).size(Button.SMALL_WIDTH, Button.DEFAULT_HEIGHT).build();
		
		LinearLayout layout = LinearLayout.vertical().spacing(ClientCensorCraft.PADDING / 2);
		layout.defaultCellSetting().alignHorizontallyCenter();
		
		// Everything else is aligned to the left
		layout.addChild(Checkbox.builder(Component.literal("Show speech"), font).tooltip(Tooltip.create(Component.literal("Displays live audio transcriptions"))).selected(ClientConfig.get().isShowTranscription()).onValueChange((button, value) -> ClientConfig.get().setShowTranscription(value)).build());
		
		layout.addChild(new OptionInstance<Integer>(null, OptionInstance.cachedConstantTooltip(Component.literal("Latency")), (component, value) -> switch(value)
		{
			case ClientConfig.MIN_LATENCY -> Component.literal("Fast");
			case ClientConfig.MAX_LATENCY -> Component.literal("Slow");
			default -> Component.literal(value + "ms");
		}, new OptionInstance.IntRange(ClientConfig.MIN_LATENCY, ClientConfig.MAX_LATENCY), ClientConfig.get().getLatency(), ClientConfig.get()::setLatency).createButton(minecraft.options)); // no clue what minecraft.options is
		
		// Add warning about latency
		Component component = Component.literal("Lower latency = faster transcriptions, but more intensive on hardware");
		layout.addChild(new MultiLineTextWidget(component, font).setMaxWidth(optionsWidth));
		
		layout.addChild(new SpacerElement(optionsWidth, 6));
		
		layout.addChild(Button.builder(Component.literal("Open models folder"), pButton -> Util.getPlatform().openPath(ClientCensorCraft.INSTANCE.getModelDir())).build());
		
		// Experimental
		layout.addChild(new SpacerElement(optionsWidth, 12));
		layout.addChild(new StringWidget(Component.literal("Experimental"), font));
		layout.addChild(Checkbox.builder(Component.literal("Use GPU"), font).tooltip(Tooltip.create(Component.literal("Use Vulkan for GPU acceleration. GPU driver must support Vulkan"))).selected(ClientConfig.get().isUseVulkan()).onValueChange((button, value) ->
		{
			ClientConfig.get().setUseVulkan(value);
			
			if(Transcriber.libraryLoaded)
			{
				minecraft.setScreen(new PopupScreen.Builder(this, Component.literal("Requires restart")).setMessage(Component.literal("Libraries are already loaded. Restart Minecraft for changes to take effect.")).addButton(CommonComponents.GUI_OK, PopupScreen::onClose).build());
			}
		}).build());
		
		layout.addChild(Checkbox.builder(Component.literal("Debug"), font).tooltip(Tooltip.create(Component.literal("Displays useful debugging information"))).selected(ClientConfig.get().isDebug()).onValueChange((button, value) -> ClientConfig.get().setDebug(value)).build());
		
		layout.addChild(Button.builder(Component.literal("Open config file"), pButton -> Util.getPlatform().openPath(ClientConfig.get().getFilePath())).build());//.active = ClientConfig.filePath != null;
		
		LinearLayout buttonLayout = LinearLayout.horizontal().spacing(ClientCensorCraft.PADDING);
		
		// Put it together
		buttonLayout.addChild(restartButton);
		
		// Close button
		buttonLayout.addChild(Button.builder(Component.literal("Close"), button -> this.onClose()).size(Button.BIG_WIDTH, Button.DEFAULT_HEIGHT).build());
		
		buttonLayout.arrangeElements();
		
		int layoutX = (this.width - buttonLayout.getWidth()) / 2;
		int layoutY = this.height - (ClientCensorCraft.PADDING + Button.DEFAULT_HEIGHT);
		buttonLayout.setPosition(layoutX, layoutY);
		
		buttonLayout.visitWidgets(this::addRenderableWidget);
		
		// List of options on right
		int optionsX = ClientCensorCraft.PADDING;
		addRenderableWidget(new ScrollArea(layout, optionsX, listY, optionsWidth, layoutY - listY - ClientCensorCraft.PADDING * 2));
		
		// Add text
		addRenderableWidget(new StringWidget(optionsX, ClientCensorCraft.PADDING, optionsWidth, font.lineHeight, Component.literal("Options"), font));
	}
}