package io.github.freshsupasulley.censorcraft.gui;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import io.github.freshsupasulley.censorcraft.config.Config;
import io.github.freshsupasulley.censorcraft.gui.MicrophoneList.MicrophoneEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public class MicrophoneList extends ObjectSelectionList<MicrophoneEntry> {
	
	private static final int TEXT_PADDING = 2;
	
	private Button refreshButton;
	
	public MicrophoneList(Button refreshButton, int x, int y, int width, int height, Minecraft minecraft, Collection<String> microphones)
	{
		// Winning the world's dumbest constructor award
		super(minecraft, width, height, y, TEXT_PADDING * 3 + minecraft.font.lineHeight * microphones.stream().mapToInt(name -> minecraft.font.split(Component.literal(name), width).size()).max().orElseThrow());
		
		this.refreshButton = refreshButton;
		
		microphones.forEach(name -> addEntry(new MicrophoneEntry(name)));
		// Select the desired, otherwise just the first one
		setSelected(this.children().stream().filter(mic -> mic.component.getString().equals(Config.Client.PREFERRED_MIC.get())).findFirst().orElse(this.getFirstElement()));
		setX(x);
	}
	
	@Override
	public void setSelected(MicrophoneEntry element)
	{
		super.setSelected(element);
		String newMic = Optional.ofNullable(element).map(entry -> entry.getMicrophoneName()).orElse("");
		
		if(!newMic.equals(Config.Client.PREFERRED_MIC.get()))
		{
			CensorCraft.LOGGER.info("Client changed preferred audio source to \"{}\"", newMic);
			Config.Client.PREFERRED_MIC.set(newMic);
			refreshButton.onPress();
		}
	}
	
	@Override
	public int getRowWidth()
	{
		return this.width;
	}
	
	class MicrophoneEntry extends ObjectSelectionList.Entry<MicrophoneEntry> {
		
		private final String micName;
		private final Component component;
		private List<FormattedCharSequence> strings;
		
		private MicrophoneEntry(String name)
		{
			this.micName = name;
			this.component = Component.literal(name);
			this.strings = minecraft.font.split(component, width);
		}
		
		/**
		 * @return name of the microphone
		 */
		public String getMicrophoneName()
		{
			return micName;
		}
		
		@Override
		public void render(GuiGraphics guiGraphics, int entryIdx, int top, int left, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean p_194999_5_, float partialTick)
		{
			int y = top + TEXT_PADDING;
			for(var string : strings)
			{
				guiGraphics.drawString(minecraft.font, string, left + (width / 2F) - (minecraft.font.width(string) / 2F), y, 0xFFFFFF, false);
				y += minecraft.font.lineHeight;
			}
		}
		
		@Override
		public Component getNarration()
		{
			return Component.translatable("narrator.select", component);
		}
	}
}
