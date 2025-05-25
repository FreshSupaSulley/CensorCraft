package io.github.freshsupasulley.censorcraft.gui;

import io.github.freshsupasulley.censorcraft.ClientCensorCraft;
import io.github.freshsupasulley.censorcraft.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractOptionSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public class InputSensitivity extends AbstractOptionSliderButton {
	
	private final OptionInstance<Integer> instance = new OptionInstance<Integer>(null, OptionInstance.cachedConstantTooltip(Component.literal("Input sensitivity")), (component, value) -> Component.literal(value + "%"), new OptionInstance.IntRange(ClientConfig.MIN_IS * 100, ClientConfig.MAX_IS * 100), (int) (ClientConfig.INPUT_SENSITIVITY.get() * 100), (value) ->
	{
		ClientConfig.INPUT_SENSITIVITY.set(value / 100f);
	});
	
	private Button restartButton;
	
	public InputSensitivity(Options pOptions, Button restartButton)
	{
		super(pOptions, 0, 0, 150, 20, ClientConfig.INPUT_SENSITIVITY.get()); // default values
		
		this.restartButton = restartButton;
		this.setTooltip(Tooltip.create(Component.literal("Input sensitivity")));
		this.updateMessage();
	}
	
	@Override
	protected void updateMessage()
	{
		this.setMessage(Component.literal(instance.get() + "%"));
	}
	
	@Override
	protected void applyValue()
	{
		restartButton.onPress();
		instance.set((int) Math.round(value * 100));
	}
	
	@Override
	public void renderWidget(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick)
	{
		Minecraft minecraft = Minecraft.getInstance();
		graphics.blitSprite(RenderType::guiTextured, this.getSprite(), this.getX(), this.getY(), this.getWidth(), this.getHeight(), ARGB.white(this.alpha));
		
		// Now draw the volume
		graphics.fill(RenderType.guiOverlay(), this.getX(), this.getY(), this.getX() + (int) (ClientCensorCraft.JSCRIBE_VOLUME * this.getWidth()), this.getY() + this.getHeight(), ClientCensorCraft.JSCRIBE_VOLUME >= (instance.get() / 100f) ? 0x3300FF00 : 0x33FFFFFF);
		
		graphics.blitSprite(RenderType::guiTextured, this.getHandleSprite(), this.getX() + (int) (this.value * (double) (this.width - 8)), this.getY(), 8, this.getHeight(), ARGB.white(this.alpha));
		int i = this.active ? 16777215 : 10526880;
		this.renderScrollingString(graphics, minecraft.font, 2, i | Mth.ceil(this.alpha * 255.0F) << 24);
		
		// Harvested from
		// super.renderWidget(p_332467_, p_329907_, p_334179_, p_329288_);
	}
}