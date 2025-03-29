package com.supasulley.censorcraft.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.supasulley.censorcraft.CensorCraft;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

@Mixin(Gui.class)
public abstract class GUIMixin {
	
//	private static final ResourceLocation MICROPHONE_ICON = ResourceLocation.fromNamespaceAndPath(CensorCraft.MODID, "textures/microphone.png");
	private static final int PADDING = 5;
	
//	@Inject(method = "tick", at = @At("RETURN"))
//	private void tick(CallbackInfo info)
//	{
//		
//	}
	
	@Inject(method = "renderHotbarAndDecorations", at = @At("RETURN"))
	private void renderHotbarAndDecorations(GuiGraphics graphics, DeltaTracker tracker, CallbackInfo info)
	{
		MutableComponent component = Component.empty();
		
		if(CensorCraft.GUI_TEXT != null)
		{
			component.append(CensorCraft.GUI_TEXT);
		}
		
		graphics.drawWordWrap(Minecraft.getInstance().font, component, PADDING, PADDING, graphics.guiWidth() - PADDING * 2, 16777215);
	}
	
	// Thank you https://github.com/henkelmax/simple-voice-chat/blob/1.21.5/common-client/src/main/java/de/maxhenkel/voicechat/voice/client/RenderEvents.java
//	private void renderIcon(GuiGraphics guiGraphics, ResourceLocation texture)
//	{
//		guiGraphics.pose().pushPose();
//		RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
//		
//		final int scale = 8;
//		int posX = PADDING;
//		int posY = guiGraphics.guiHeight() - scale - PADDING;
//		
//		guiGraphics.pose().translate(posX, posY, 0D);
////		guiGraphics.pose().scale(scale, scale, 1F);
//		
//		guiGraphics.blit(RenderType::guiTextured, texture, 0, 0, 0, 0, scale, scale, scale, scale);
//		guiGraphics.pose().popPose();
//	}
}
