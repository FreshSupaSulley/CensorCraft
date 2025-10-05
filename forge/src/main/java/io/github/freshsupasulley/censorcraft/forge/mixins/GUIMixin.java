package io.github.freshsupasulley.censorcraft.forge.mixins;

import io.github.freshsupasulley.censorcraft.common.ClientCensorCraft;
import io.github.freshsupasulley.censorcraft.common.config.ClientConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

@Mixin(Gui.class)
public abstract class GUIMixin {
	
	private static final long GUI_TIMEOUT = 10000;
	
	private Component lastGuiComponent;
	private long lastGuiUpdate;
	
	@Inject(method = "renderHotbarAndDecorations", at = @At("RETURN"))
	private void renderHotbarAndDecorations(GuiGraphics graphics, DeltaTracker tracker, CallbackInfo info)
	{
		MutableComponent component = Component.empty();
		
		// If there's text to display and we're not timed out for repetitive messages
		if(ClientCensorCraft.GUI_TEXT != null)
		{
			// If it's a new component
			if(ClientCensorCraft.FORCE_GUI_REFRESH || !Optional.ofNullable(lastGuiComponent).orElse(Component.empty()).equals(ClientCensorCraft.GUI_TEXT))
			{
				ClientCensorCraft.FORCE_GUI_REFRESH = false;
				lastGuiUpdate = System.currentTimeMillis();
			}
			
			if(System.currentTimeMillis() - lastGuiUpdate < GUI_TIMEOUT)
			{
				component.append(ClientCensorCraft.GUI_TEXT);
			}
			
			lastGuiComponent = ClientCensorCraft.GUI_TEXT;
		}
		
		Minecraft minecraft = Minecraft.getInstance();
		
		int xPos = ClientConfig.get().getGUIX();
		drawAligned(graphics, minecraft.font, component, xPos, ClientCensorCraft.PADDING, graphics.guiWidth() - ClientCensorCraft.PADDING * 2, 0xFFFFFFFF);
	}
	
	private static void drawAligned(GuiGraphics graphics, Font font, Component text, int xPos, int yPos, int wrapWidth, int colorARGB)
	{
		// Split the component into wrapped lines
		List<FormattedCharSequence> lines = font.split(text, wrapWidth);
		int y = yPos;
		
		for(FormattedCharSequence line : lines)
		{
			int lineWidth = font.width(line);
			
			// Positive = left, negative = right
			int x = xPos >= 0 ? xPos : graphics.guiWidth() + xPos - lineWidth;
			graphics.drawString(font, line, x, y, colorARGB, true);
			y += font.lineHeight;
		}
	}
}
