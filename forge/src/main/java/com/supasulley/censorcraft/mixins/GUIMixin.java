package com.supasulley.censorcraft.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.supasulley.censorcraft.ClientCensorCraft;
import com.supasulley.censorcraft.Config;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

@Mixin(Gui.class)
public abstract class GUIMixin {
	
	// private static final ResourceLocation MICROPHONE_ICON = ResourceLocation.fromNamespaceAndPath(CensorCraft.MODID, "textures/microphone.png");
	// @Inject(method = "tick", at = @At("RETURN"))
	// private void tick(CallbackInfo info)
	// {
	//
	// }
	
	@Inject(method = "renderHotbarAndDecorations", at = @At("RETURN"))
	private void renderHotbarAndDecorations(GuiGraphics graphics, DeltaTracker tracker, CallbackInfo info)
	{
		MutableComponent component = Component.empty();
		
		if(ClientCensorCraft.GUI_TEXT != null)
		{
			component.append(ClientCensorCraft.GUI_TEXT);
		}
		
		Minecraft minecraft = Minecraft.getInstance();
		
		final int barWidth = 5, barHeight = minecraft.font.lineHeight * 2;
		int x = ClientCensorCraft.PADDING;
		
		if(Config.Client.SHOW_VOLUME_BAR.get())
		{
			x += barWidth + ClientCensorCraft.PADDING;
			graphics.fill(RenderType.guiOverlay(), ClientCensorCraft.PADDING, ClientCensorCraft.PADDING + barHeight, ClientCensorCraft.PADDING + barWidth, ClientCensorCraft.PADDING + barHeight - Math.clamp((int) (ClientCensorCraft.JSCRIBE_VOLUME * barHeight), 1, barHeight), 0xAAFFFFFF);
		}
		
		if(Config.Client.SHOW_VAD.get() && ClientCensorCraft.SPEAKING)
		{
			component.append(Component.literal("\nSpeaking"));
		}
		
		// Color is ARGB
		graphics.drawWordWrap(minecraft.font, component, x, ClientCensorCraft.PADDING, graphics.guiWidth() - x - ClientCensorCraft.PADDING, 0xFFFFFFFF);
	}
	
	// /**
	// * Draws a solid color rectangle with the specified coordinates and color. This variation does not use GL_BLEND.
	// *
	// * @param x1 the first x-coordinate of the rectangle
	// * @param y1 the first y-coordinate of the rectangle
	// * @param x2 the second x-coordinate of the rectangle
	// * @param y2 the second y-coordinate of the rectangle
	// * @param color the color of the rectangle
	// * @param zLevel the z-level of the graphic
	// * @see net.minecraft.client.gui.Gui#drawRect(int, int, int, int, int)
	// */
	// private static void drawRectNoBlend(int x1, int y1, int x2, int y2, int color, float zLevel)
	// {
	// int temp;
	//
	// if(x1 < x2)
	// {
	// temp = x1;
	// x1 = x2;
	// x2 = temp;
	// }
	//
	// if(y1 < y2)
	// {
	// temp = y1;
	// y1 = y2;
	// y2 = temp;
	// }
	//
	// RenderSystem.enableBlend();
	// RenderSystem.defaultBlendFunc();
	// RenderSystem.setShader(CoreShaders.POSITION_COLOR);
	//
	// Tesselator tessellator = Tesselator.getInstance();
	// BufferBuilder builder = tessellator.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
	// builder.addVertex(x1, y2, zLevel).setColor(color);
	// builder.addVertex(x2, y2, zLevel).setColor(color);
	// builder.addVertex(x2, y1, zLevel).setColor(color);
	// builder.addVertex(x1, y1, zLevel).setColor(color);
	//
	// BufferUploader.drawWithShader(builder.buildOrThrow());
	// RenderSystem.disableBlend();
	// }
	
	// Thank you https://github.com/henkelmax/simple-voice-chat/blob/1.21.5/common-client/src/main/java/de/maxhenkel/voicechat/voice/client/RenderEvents.java
	// private void renderIcon(GuiGraphics guiGraphics, ResourceLocation texture)
	// {
	// guiGraphics.pose().pushPose();
	// RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
	//
	// final int scale = 8;
	// int posX = PADDING;
	// int posY = guiGraphics.guiHeight() - scale - PADDING;
	//
	// guiGraphics.pose().translate(posX, posY, 0D);
	//// guiGraphics.pose().scale(scale, scale, 1F);
	//
	// guiGraphics.blit(RenderType::guiTextured, texture, 0, 0, 0, 0, scale, scale, scale, scale);
	// guiGraphics.pose().popPose();
	// }
}
