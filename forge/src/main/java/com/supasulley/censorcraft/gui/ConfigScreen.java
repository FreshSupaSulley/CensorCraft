package com.supasulley.censorcraft.gui;

import java.util.stream.Collectors;

import com.supasulley.censorcraft.Config;

import io.github.freshsupasulley.JScribe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {
	
	private static final int PADDING = 5;
	
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
		final int listY = PADDING * 2 + font.lineHeight;
		final int listSpacing = 40;
		final int micListWidth = this.width / 3;
		final int optionsWidth = this.width - micListWidth - listSpacing - PADDING;
		
		GridLayout grid = new GridLayout();
		grid.rowSpacing(PADDING);
		grid.defaultCellSetting().alignHorizontallyLeft();
		
		// Add all config options!
		GridLayout.RowHelper layout = grid.createRowHelper(1);
		// Put the restart button in the center with some padding (and conveniently we can set the max width here too for proper centering)
		// layout.addChild(restartButton, grid.newCellSettings().alignHorizontallyCenter());
		layout.addChild(new SpacerElement(optionsWidth, 1));
		
		// Everything else is aligned to the left
		layout.addChild(Checkbox.builder(Component.literal("Show speech"), font).tooltip(Tooltip.create(Component.literal("Displays live audio transcriptions"))).selected(Config.Client.SHOW_TRANSCRIPTION.get()).onValueChange((button, value) ->
		{
			Config.Client.SHOW_TRANSCRIPTION.set(value);
		}).build());
		
		layout.addChild(Checkbox.builder(Component.literal("Show microphone volume"), font).tooltip(Tooltip.create(Component.literal("Displays a volume bar indicating your microphone volume"))).selected(Config.Client.SHOW_VOLUME_BAR.get()).onValueChange((button, value) ->
		{
			Config.Client.SHOW_VOLUME_BAR.set(value);
		}).build());
		
		layout.addChild(Checkbox.builder(Component.literal("Show voice detection"), font).tooltip(Tooltip.create(Component.literal("Indicates when speech is detected"))).selected(Config.Client.VAD.get()).onValueChange((button, value) ->
		{
			Config.Client.SHOW_VAD.set(value);
		}).build()).active = Config.Client.VAD.get();
		
		layout.addChild(Checkbox.builder(Component.literal("Show delay"), font).tooltip(Tooltip.create(Component.literal("Displays how far behind transcription is"))).selected(Config.Client.SHOW_DELAY.get()).onValueChange((button, value) ->
		{
			Config.Client.SHOW_DELAY.set(value);
		}).build());
		
		// layout.addChild(new ForgeSlider(0, 0, optionsWidth, java.awt.Button.HEIGHT, Component.literal("prefix"), Component.literal("suffix"), 30, 1500,
		AbstractWidget slider = new OptionInstance<>(null, OptionInstance.cachedConstantTooltip(Component.literal("Transcription speed")), (component, value) ->
		{
			return switch(value)
			{
				case 30 -> Component.literal("Fast");
				case 1500 -> Component.literal("Slow");
				default -> Component.literal(value + "ms");
			};
		}, new OptionInstance.IntRange(30, 1500), Config.Client.LATENCY.get(), Config.Client.LATENCY::set).createButton(minecraft.options); // no clue what minecraft.options is
		
		slider.setWidth(optionsWidth);
		layout.addChild(slider);
		
		LinearLayout buttonLayout = LinearLayout.horizontal().spacing(PADDING);
		
		// Put it together
		Button restartButton = buttonLayout.addChild(Button.builder(Component.literal("Restart"), button ->
		{
			button.active = false;
			restartJScribe = true;
		}).size(Button.SMALL_WIDTH, Button.DEFAULT_HEIGHT).build());//.bounds(PADDING, this.height - Button.DEFAULT_HEIGHT - PADDING, Button.SMALL_WIDTH, Button.DEFAULT_HEIGHT).build();
		
		// Close button
		buttonLayout.addChild(Button.builder(Component.literal("Close"), button -> this.onClose()).size(Button.BIG_WIDTH, Button.DEFAULT_HEIGHT).build()).getY();
		
		buttonLayout.arrangeElements();
		
		int layoutX = (this.width - buttonLayout.getWidth()) / 2;
		int layoutY = this.height - (PADDING + Button.DEFAULT_HEIGHT);
		buttonLayout.setPosition(layoutX, layoutY);
		
		buttonLayout.visitWidgets(this::addRenderableWidget);
		
		// List of microphones on left
		list = new MicrophoneList(restartButton, PADDING, listY, micListWidth - PADDING * 2, layoutY - listY - PADDING, minecraft, JScribe.getMicrophones().stream().map(mic -> mic.getName()).collect(Collectors.toList()));
		addRenderableWidget(list);
		
		// List of options on right
		int optionsX = addRenderableWidget(new ScrollArea(grid, list.getRight() + listSpacing + PADDING, listY, optionsWidth, layoutY - listY - PADDING * 2)).getX();
		
		// Add text
		addRenderableWidget(new StringWidget(PADDING, PADDING, micListWidth, font.lineHeight, Component.literal("Select Microphone"), font));
		addRenderableWidget(new StringWidget(optionsX, PADDING, optionsWidth, font.lineHeight, Component.literal("Options"), font));
	}
}
