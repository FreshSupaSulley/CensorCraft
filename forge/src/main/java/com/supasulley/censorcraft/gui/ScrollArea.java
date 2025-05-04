package com.supasulley.censorcraft.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

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

/**
 * Stolen from net.minecraft.client.gui.screens.worldselection.ExperimentsScreen.ScrollArea
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
	protected void renderWidget(GuiGraphics p_376202_, int p_375703_, int p_376669_, float p_377299_)
	{
		p_376202_.enableScissor(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height);
		p_376202_.pose().pushPose();
		p_376202_.pose().translate(0.0, -this.scrollAmount(), 0.0);
		
		for(AbstractWidget abstractwidget : this.children)
		{
			abstractwidget.render(p_376202_, p_375703_, p_376669_, p_377299_);
		}
		
		p_376202_.pose().popPose();
		p_376202_.disableScissor();
		this.renderScrollbar(p_376202_);
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
	public void setFocused(@Nullable GuiEventListener p_375407_)
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
