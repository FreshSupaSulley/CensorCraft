package com.supasulley.censorcraft.gui;

import java.util.Optional;
import java.util.stream.Collectors;

import com.supasulley.censorcraft.CensorCraft;
import com.supasulley.censorcraft.Config;
import com.supasulley.jscribe.AudioRecorder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// inject gui class
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
	 * Ran everytime the config button is pressed.
	 */
	@Override
	protected void init()
	{
		// int bottom = addRenderableWidget(Checkbox.builder(Component.literal("Indicate recording"), font).tooltip(Tooltip.create(Component.literal("Shows permanent
		// text in-game indicating recording status"))).pos(PADDING, PADDING).selected(Config.Client.INDICATE_RECORDING.get()).onValueChange((button, value) ->
		// {
		// Config.Client.INDICATE_RECORDING.set(value);
		// }).build()).getBottom();
		
		int bottom = addRenderableWidget(Checkbox.builder(Component.literal("Show spoken text"), font).pos(PADDING, PADDING).tooltip(Tooltip.create(Component.literal("Displays live audio transcriptions"))).selected(Config.Client.SHOW_TRANSCRIPTION.get()).onValueChange((button, value) ->
		{
			Config.Client.SHOW_TRANSCRIPTION.set(value);
		}).build()).getBottom();
		
		final int restartButtonY = this.height - (PADDING + Button.DEFAULT_HEIGHT) * 2;
		final int listWidth = this.width / 2;
		
		// List of microphones
		list = new MicrophoneList(PADDING + listWidth / 2, bottom + PADDING, listWidth - PADDING * 2, restartButtonY - (bottom + PADDING) - PADDING, minecraft, AudioRecorder.getMicrophones().stream().map(mic -> mic.getName()).collect(Collectors.toList()));
		addRenderableWidget(list);
		
		System.out.println(list.getY() + " " + this.height + " " + list.getHeight());
		// Restart button
		bottom = addRenderableWidget(Button.builder(Component.literal("Restart"), button ->
		{
			button.active = false;
			restartJScribe = true;
		}).bounds(this.width / 2 - Button.BIG_WIDTH / 2, restartButtonY, Button.BIG_WIDTH, Button.DEFAULT_HEIGHT).build()).getBottom();
		
		// Close button
		addRenderableWidget(Button.builder(Component.literal("Close"), button -> this.onClose()).bounds(this.width / 2 - Button.BIG_WIDTH / 2, bottom + PADDING, Button.BIG_WIDTH, Button.DEFAULT_HEIGHT).build());
	}
	
	@Override
	public void onClose()
	{
		super.onClose();
		
		// Ensure to update preferred mic
		String newMic = Optional.ofNullable(list.getSelected()).map(entry -> entry.getMicrophoneName()).orElse("");
		
		if(!newMic.equals(Config.Client.PREFERRED_MIC.get()))
		{
			CensorCraft.LOGGER.info("Client changed preferred audio source to \"{}\"", newMic);
			ConfigScreen.restartJScribe = true;
			Config.Client.PREFERRED_MIC.set(newMic);
		}
	}
	
	@Override
	public void render(GuiGraphics p_281549_, int p_281550_, int p_282878_, float p_282465_)
	{
		super.render(p_281549_, p_281550_, p_282878_, p_282465_);
	}
}
