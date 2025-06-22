package io.github.freshsupasulley.censorcraft.gui;

import io.github.freshsupasulley.LibraryLoader;
import io.github.freshsupasulley.censorcraft.ClientCensorCraft;
import io.github.freshsupasulley.censorcraft.config.ClientConfig;
import io.github.freshsupasulley.censorcraft.config.CCConfig;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
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
	private static boolean queuedRestart, restartJScribe;
	
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
	
	@Override
	public void onClose()
	{
		restartJScribe = queuedRestart;
		queuedRestart = false;
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
		
		Button restartButton = Button.builder(Component.literal("Restart"), button ->
		{
			button.active = false;
			queuedRestart = true;
		}).size(Button.SMALL_WIDTH, Button.DEFAULT_HEIGHT).build();
		
		LinearLayout layout = LinearLayout.vertical().spacing(ClientCensorCraft.PADDING / 2);
		layout.defaultCellSetting().alignHorizontallyCenter();
		
		// Everything else is aligned to the left
		layout.addChild(Checkbox.builder(Component.literal("Show speech"), font).tooltip(Tooltip.create(Component.literal("Displays live audio transcriptions"))).selected(ClientConfig.SHOW_TRANSCRIPTION.get()).onValueChange((button, value) ->
		{
			ClientConfig.SHOW_TRANSCRIPTION.set(value);
		}).build());
		
//		layout.addChild(Checkbox.builder(Component.literal("Indicate when transcribing"), font).tooltip(Tooltip.create(Component.literal("Text appears when voice is detected and audio meets input sensitivity"))).selected(ClientConfig.INDICATE_TRANSCRIBING.get()).onValueChange((button, value) ->
//		{
//			ClientConfig.INDICATE_TRANSCRIBING.set(value);
//		}).build());
		
		layout.addChild(new OptionInstance<Integer>(null, OptionInstance.cachedConstantTooltip(Component.literal("Latency")), (component, value) ->
		{
			return switch(value)
			{
				case ClientConfig.MIN_LATENCY -> Component.literal("Fast");
				case ClientConfig.MAX_LATENCY -> Component.literal("Slow");
				default -> Component.literal(value + "ms");
			};
		}, new OptionInstance.IntRange(ClientConfig.MIN_LATENCY, ClientConfig.MAX_LATENCY), ClientConfig.LATENCY.get(), ClientConfig.LATENCY::set).createButton(minecraft.options)); // no clue what minecraft.options is
		
		// I actually prefer the default width more
		// slider.setWidth(optionsWidth - 10); // remove a bit so it doesnt intersect the slider
		
		// Add warning about latency
		Component component = Component.literal("Lower latency = faster transcriptions, but more intensive on hardware");
		layout.addChild(new MultiLineTextWidget(component, font).setMaxWidth(optionsWidth));
		
		layout.addChild(new SpacerElement(optionsWidth, 6));
		
		layout.addChild(Button.builder(Component.literal("Open models folder"), pButton -> Util.getPlatform().openPath(ClientCensorCraft.getModelDir())).build());
		
		// Experimental
		layout.addChild(new SpacerElement(optionsWidth, 12));
		layout.addChild(new StringWidget(Component.literal("Experimental"), font));
		layout.addChild(Checkbox.builder(Component.literal("Use GPU (Windows x64 only)"), font).tooltip(Tooltip.create(Component.literal("Use Vulkan for GPU acceleration. GPU driver must support Vulkan"))).selected(ClientConfig.USE_VULKAN.get() && LibraryLoader.canUseVulkan()).onValueChange((button, value) ->
		{
			ClientConfig.USE_VULKAN.set(value);
			
			if(ClientCensorCraft.librariesLoaded)
			{
				minecraft.setScreen(new PopupScreen.Builder(this, Component.literal("Requires restart")).setMessage(Component.literal("Libraries are already loaded. Restart Minecraft for changes to take effect.")).addButton(CommonComponents.GUI_OK, PopupScreen::onClose).build());
			}
		}).build()).active = LibraryLoader.canUseVulkan();
		
		layout.addChild(Checkbox.builder(Component.literal("Debug"), font).tooltip(Tooltip.create(Component.literal("Displays useful debugging information"))).selected(ClientConfig.DEBUG.get()).onValueChange((button, value) ->
		{
			ClientConfig.DEBUG.set(value);
		}).build());
		
		layout.addChild(Button.builder(Component.literal("Open config file"), pButton -> Util.getPlatform().openPath(CCConfig.CLIENT.getFilePath())).build());//.active = ClientConfig.filePath != null;
		
		LinearLayout buttonLayout = LinearLayout.horizontal().spacing(ClientCensorCraft.PADDING);
		
		// Put it together
		buttonLayout.addChild(restartButton);
		
		// Close button
		buttonLayout.addChild(Button.builder(Component.literal("Close"), button -> this.onClose()).size(Button.BIG_WIDTH, Button.DEFAULT_HEIGHT).build()).getY();
		
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