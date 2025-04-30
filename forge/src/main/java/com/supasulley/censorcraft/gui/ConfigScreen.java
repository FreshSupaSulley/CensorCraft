package com.supasulley.censorcraft.gui;

import java.util.stream.Collectors;

import com.supasulley.censorcraft.Config;

import io.github.freshsupasulley.AudioRecorder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.Tooltip;
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
		// int bottom = addRenderableWidget(Checkbox.builder(Component.literal("Indicate recording"), font).tooltip(Tooltip.create(Component.literal("Shows permanent
		// text in-game indicating recording status"))).pos(PADDING, PADDING).selected(Config.Client.INDICATE_RECORDING.get()).onValueChange((button, value) ->
		// {
		// Config.Client.INDICATE_RECORDING.set(value);
		// }).build()).getBottom();
		
		Checkbox checkbox = addRenderableWidget(Checkbox.builder(Component.literal("Show spoken text"), font).tooltip(Tooltip.create(Component.literal("Displays live audio transcriptions"))).selected(Config.Client.SHOW_TRANSCRIPTION.get()).onValueChange((button, value) ->
		{
			Config.Client.SHOW_TRANSCRIPTION.set(value);
		}).build());
		
		Checkbox volumeBar = addRenderableWidget(Checkbox.builder(Component.literal("Show microphone volume"), font).tooltip(Tooltip.create(Component.literal("Displays a volume bar indicating your microphone volume"))).selected(Config.Client.SHOW_VOLUME_BAR.get()).onValueChange((button, value) ->
		{
			Config.Client.SHOW_VOLUME_BAR.set(value);
		}).build());
		
		// Combine them
		int totalCheckboxWidth = checkbox.getWidth() + volumeBar.getWidth();
		
		checkbox.setPosition(width / 2 - totalCheckboxWidth / 2 - PADDING, PADDING);
		volumeBar.setPosition(width / 2 + PADDING, PADDING);
		
		int bottom = checkbox.getBottom();
		
		final int restartButtonY = this.height - (PADDING + Button.DEFAULT_HEIGHT) * 2;
		final int listWidth = this.width / 2;
		
		// Restart button defined above list because list requires it
		Button restartButton = Button.builder(Component.literal("Restart"), button ->
		{
			button.active = false;
			restartJScribe = true;
		}).bounds(this.width / 2 - Button.BIG_WIDTH / 2, restartButtonY, Button.BIG_WIDTH, Button.DEFAULT_HEIGHT).build();
		
		// List of microphones
		list = new MicrophoneList(restartButton, PADDING + listWidth / 2, bottom + PADDING, listWidth - PADDING * 2, restartButtonY - (bottom + PADDING) - PADDING, minecraft, AudioRecorder.getMicrophones().stream().map(mic -> mic.getName()).collect(Collectors.toList()));
		bottom = restartButton.getBottom();
		addRenderableWidget(list);
		
		// Add button after widget
		addRenderableWidget(restartButton);
		
		// Close button
		addRenderableWidget(Button.builder(Component.literal("Close"), button -> this.onClose()).bounds(this.width / 2 - Button.BIG_WIDTH / 2, bottom + PADDING, Button.BIG_WIDTH, Button.DEFAULT_HEIGHT).build());
	}
}
