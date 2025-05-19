package io.github.freshsupasulley.censorcraft.gui;

import java.util.stream.Collectors;

import io.github.freshsupasulley.JScribe;
import io.github.freshsupasulley.LibraryLoader;
import io.github.freshsupasulley.censorcraft.ClientCensorCraft;
import io.github.freshsupasulley.censorcraft.config.Config;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {
	
	private Minecraft minecraft;
	private MicrophoneList list;
	private static boolean restartJScribe;
	
	public ConfigScreen(Minecraft minecraft, Screen screen)
	{
		super(Component.literal("Configure CensorCraft"));
		
		this.minecraft = minecraft;
	}
	
	/**
	 * @return true if audio transcription should be restarted, false if good to keep going
	 */
	public static boolean restart()
	{
		boolean result = restartJScribe;
		restartJScribe = false;
		return result;
	}
	
	/**
	 * Runs everytime the mod config menu is opened.
	 */
	@Override
	protected void init()
	{
		// Lists
		final int listY = ClientCensorCraft.PADDING * 2 + font.lineHeight;
		final int listSpacing = 40;
		final int micListWidth = this.width / 3;
		final int optionsWidth = this.width - micListWidth - listSpacing - ClientCensorCraft.PADDING;
		
		LinearLayout layout = LinearLayout.vertical().spacing(ClientCensorCraft.PADDING / 2);
		// layout.addChild(new SpacerElement(optionsWidth, 1));
		
		// Everything else is aligned to the left
		layout.addChild(Checkbox.builder(Component.literal("Show speech"), font).tooltip(Tooltip.create(Component.literal("Displays live audio transcriptions"))).selected(Config.Client.SHOW_TRANSCRIPTION.get()).onValueChange((button, value) ->
		{
			Config.Client.SHOW_TRANSCRIPTION.set(value);
		}).build());
		
		layout.addChild(Checkbox.builder(Component.literal("Show microphone volume"), font).selected(Config.Client.SHOW_VOLUME_BAR.get()).onValueChange((button, value) ->
		{
			Config.Client.SHOW_VOLUME_BAR.set(value);
		}).build());
		
		layout.addChild(Checkbox.builder(Component.literal("Indicate when speaking"), font).tooltip(Tooltip.create(Component.literal("Text appears when voice is detected"))).selected(Config.Client.SHOW_VAD.get()).onValueChange((button, value) ->
		{
			Config.Client.SHOW_VAD.set(value);
		}).build()).active = Config.Client.VAD.get();
		
		// layout.addChild(new ForgeSlider(0, 0, optionsWidth, java.awt.Button.HEIGHT, Component.literal("prefix"), Component.literal("suffix"), 30, 1500,
		AbstractWidget slider = new OptionInstance<>(null, OptionInstance.cachedConstantTooltip(Component.literal("Latency")), (component, value) ->
		{
			return switch(value)
			{
				case Config.Client.MIN_LATENCY -> Component.literal("Fast");
				case Config.Client.MAX_LATENCY -> Component.literal("Slow");
				default -> Component.literal(value + "ms");
			};
		}, new OptionInstance.IntRange(Config.Client.MIN_LATENCY, Config.Client.MAX_LATENCY), Config.Client.LATENCY.get(), Config.Client.LATENCY::set).createButton(minecraft.options); // no clue what minecraft.options is
		
		slider.setWidth(optionsWidth);
		layout.addChild(slider);
		
		// Add warning about latency
		Component component = Component.literal("Lower latency = faster transcriptions, but more intensive on hardware");
		layout.addChild(new MultiLineTextWidget(component, font).setMaxWidth(optionsWidth));
		
		layout.addChild(new SpacerElement(optionsWidth, 6));
		
		layout.addChild(Button.builder(Component.literal("Open models folder"), pButton -> Util.getPlatform().openPath(ClientCensorCraft.getModelDir())).build());
		
		// Experimental
		layout.addChild(new SpacerElement(optionsWidth, 6));
		layout.addChild(new StringWidget(Component.literal("Experimental"), font));
		layout.addChild(Checkbox.builder(Component.literal("Use GPU (Windows x64 only)"), font).tooltip(Tooltip.create(Component.literal("Use Vulkan for GPU acceleration. GPU driver must support Vulkan"))).selected(Config.Client.USE_VULKAN.get() && LibraryLoader.canUseVulkan()).onValueChange((button, value) ->
		{
			Config.Client.USE_VULKAN.set(value);
			
			if(ClientCensorCraft.librariesLoaded)
			{
				minecraft.setScreen(new PopupScreen.Builder(this, Component.literal("Requires restart")).setMessage(Component.literal("Libraries are already loaded. Restart Minecraft for changes to take effect.")).addButton(CommonComponents.GUI_OK, PopupScreen::onClose).build());
			}
		}).build()).active = LibraryLoader.canUseVulkan();
		
		layout.addChild(Checkbox.builder(Component.literal("Debug"), font).tooltip(Tooltip.create(Component.literal("Displays useful debugging information"))).selected(Config.Client.DEBUG.get()).onValueChange((button, value) ->
		{
			Config.Client.DEBUG.set(value);
		}).build());
		
		LinearLayout buttonLayout = LinearLayout.horizontal().spacing(ClientCensorCraft.PADDING);
		
		// Put it together
		Button restartButton = buttonLayout.addChild(Button.builder(Component.literal("Restart"), button ->
		{
			button.active = false;
			restartJScribe = true;
		}).size(Button.SMALL_WIDTH, Button.DEFAULT_HEIGHT).build());// .bounds(PADDING, this.height - Button.DEFAULT_HEIGHT - PADDING, Button.SMALL_WIDTH, Button.DEFAULT_HEIGHT).build();
		
		// Close button
		buttonLayout.addChild(Button.builder(Component.literal("Close"), button -> this.onClose()).size(Button.BIG_WIDTH, Button.DEFAULT_HEIGHT).build()).getY();
		
		buttonLayout.arrangeElements();
		
		int layoutX = (this.width - buttonLayout.getWidth()) / 2;
		int layoutY = this.height - (ClientCensorCraft.PADDING + Button.DEFAULT_HEIGHT);
		buttonLayout.setPosition(layoutX, layoutY);
		
		buttonLayout.visitWidgets(this::addRenderableWidget);
		
		// List of microphones on left
		list = new MicrophoneList(restartButton, ClientCensorCraft.PADDING, listY, micListWidth - ClientCensorCraft.PADDING * 2, layoutY - listY - ClientCensorCraft.PADDING, minecraft, JScribe.getMicrophones().stream().map(mic -> mic.getName()).collect(Collectors.toList()));
		addRenderableWidget(list);
		
		// List of options on right
		int optionsX = addRenderableWidget(new ScrollArea(layout, list.getRight() + listSpacing + ClientCensorCraft.PADDING, listY, optionsWidth, layoutY - listY - ClientCensorCraft.PADDING * 2)).getX();
		
		// Add text
		addRenderableWidget(new StringWidget(ClientCensorCraft.PADDING, ClientCensorCraft.PADDING, micListWidth, font.lineHeight, Component.literal("Select Microphone"), font));
		addRenderableWidget(new StringWidget(optionsX, ClientCensorCraft.PADDING, optionsWidth, font.lineHeight, Component.literal("Options"), font));
	}
}
