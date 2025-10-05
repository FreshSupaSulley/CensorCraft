package io.github.freshsupasulley.censorcraft.common.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.CommonComponents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Adapted from net.minecraft.client.gui.screens.worldselection.ExperimentsScreen.ScrollArea
 */
public class ScrollArea extends AbstractContainerWidget {
	
	private final List<AbstractWidget> children = new ArrayList<>();
	private final Layout layout;
	
	public ScrollArea(Layout pLayout, int x, int y, int pWidth, final int pHeight)
	{
		super(0, 0, pWidth, pHeight, CommonComponents.EMPTY);
		this.layout = pLayout;
		pLayout.visitWidgets(children::add);
		setX(x);
		setY(y);
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		double adjustedY = mouseY + scrollAmount();
		return super.mouseClicked(mouseX, adjustedY, button);
	}
	
	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button)
	{
		double adjustedY = mouseY + scrollAmount();
		return super.mouseReleased(mouseX, adjustedY, button);
	}
	
	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
	{
		double adjustedY = mouseY + scrollAmount();
		return super.mouseDragged(mouseX, adjustedY, button, dragX, dragY);
	}
	
	@Override
	protected int contentHeight()
	{
		return this.layout.getHeight();
	}
	
	@Override
	protected double scrollRate()
	{
		return 10.0;
	}
	
	@Override
	protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick)
	{
		pGuiGraphics.enableScissor(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height);
		pGuiGraphics.pose().pushMatrix();
		
		for(AbstractWidget widget : this.children)
		{
			int originalY = widget.getY(); // Save the real Y position
			widget.setY(originalY - (int) this.scrollAmount());
			
			widget.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
			
			widget.setY(originalY);
		}
		
		pGuiGraphics.pose().popMatrix();
		pGuiGraphics.disableScissor();
		this.renderScrollbar(pGuiGraphics);
	}
	
	@Override
	protected void updateWidgetNarration(NarrationElementOutput p_376647_)
	{
	}
	
	@Override
	public ScreenRectangle getBorderForArrowNavigation(ScreenDirection p_378226_)
	{
		return new ScreenRectangle(this.getX(), this.getY(), this.width, this.contentHeight());
	}
	
	@Override
	public void setFocused(GuiEventListener p_375407_)
	{
		super.setFocused(p_375407_);
		if(p_375407_ != null)
		{
			ScreenRectangle screenrectangle = this.getRectangle();
			ScreenRectangle screenrectangle1 = p_375407_.getRectangle();
			int i = (int) ((double) screenrectangle1.top() - this.scrollAmount() - (double) screenrectangle.top());
			int j = (int) ((double) screenrectangle1.bottom() - this.scrollAmount() - (double) screenrectangle.bottom());
			if(i < 0)
			{
				this.setScrollAmount(this.scrollAmount() + (double) i - 14.0);
			}
			else if(j > 0)
			{
				this.setScrollAmount(this.scrollAmount() + (double) j + 14.0);
			}
		}
	}
	
	@Override
	public List<? extends GuiEventListener> children()
	{
		return this.children;
	}
	
	@Override
	public void setX(int x)
	{
		super.setX(x);
		this.layout.setX(x);
		this.layout.arrangeElements();
	}
	
	@Override
	public void setY(int y)
	{
		super.setY(y);
		this.layout.setY(y);
		this.layout.arrangeElements();
	}
	
	@Override
	public Collection<? extends NarratableEntry> getNarratables()
	{
		return this.children;
	}
}
