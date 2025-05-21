package io.github.freshsupasulley.censorcraft.gui;

import java.util.concurrent.CancellationException;

import io.github.freshsupasulley.JScribe;
import io.github.freshsupasulley.Model;
import io.github.freshsupasulley.ModelDownloader;
import io.github.freshsupasulley.censorcraft.CensorCraft;
import io.github.freshsupasulley.censorcraft.ClientCensorCraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public class DownloadScreen extends Screen {
	
	private ModelDownloader downloader;
	
	public DownloadScreen(Model model)
	{
		super(Component.literal("Downloading ").append(Component.literal(model.name()).withStyle(Style.EMPTY.withBold(true))));
		
		// Update title to include size
		downloader = JScribe.downloadModel(model.name(), ClientCensorCraft.getModelPath(model.name()), (success, exception) ->
		{
			CensorCraft.LOGGER.info("Downloaded process exited");
			
			// On success
			if(success)
			{
				minecraft.execute(() -> minecraft.setScreen(new PopupScreen.Builder(new TitleScreen(), Component.literal("Downloaded model")).setMessage(Component.literal("Reconnect to join. You can manage downloaded models in the mod config menu.")).addButton(CommonComponents.GUI_OK, PopupScreen::onClose).build()));
			}
			// On cancelled
			else if(exception instanceof CancellationException)
			{
				minecraft.execute(() -> minecraft.setScreen(new PopupScreen.Builder(new TitleScreen(), Component.literal("Downloaded cancelled")).addButton(CommonComponents.GUI_OK, PopupScreen::onClose).build()));
			}
			// On error
			else
			{
				minecraft.execute(() -> minecraft.setScreen(ClientCensorCraft.errorScreen("An error occurred downloading the model", exception)));
			}
		});
	}
	
	@Override
	public void onClose()
	{
		super.onClose();
		
		downloader.cancel();
		CensorCraft.LOGGER.info("Cancelled download");
	}
	
	@Override
	protected void init()
	{
		addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> this.onClose()).bounds(this.width / 2 - Button.BIG_WIDTH / 2, this.height - Button.DEFAULT_HEIGHT - ClientCensorCraft.PADDING, Button.BIG_WIDTH, Button.DEFAULT_HEIGHT).build());
	}
	
	@Override
	public void render(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick)
	{
		super.render(graphics, pMouseX, pMouseY, pPartialTick);
		
		if(downloader.getDownloadSize() == 0)
		{
			graphics.drawCenteredString(font, Component.literal("Starting..."), this.width / 2, this.height / 3, 0xFFFFFFFF);
		}
		// Don't draw the progress bar when done (it shows underneath the popup and looks ugly asf)
		else if(!downloader.isDone())
		{
			// Cutoff the byte suffix
			graphics.drawCenteredString(font, title, this.width / 2, this.height / 4, 0xFFFFFFFF);
			graphics.drawCenteredString(font, Component.literal(ModelDownloader.getBytesFancy(downloader.getBytesRead()) + " / " + ModelDownloader.getBytesFancy(downloader.getDownloadSize())), this.width / 2, this.height / 4 + font.lineHeight * 2, 0xFFFFFFFF);
			
			final int barWidth = 200;
			final int barHeight = 10;
			
			int x = (this.width - barWidth) / 2;
			int y = this.height / 2;
			
			final float progress = downloader.getBytesRead() * 1f / downloader.getDownloadSize();
			
			int filled = (int) (barWidth * progress);
			
			graphics.fill(x, y, x + barWidth, y + barHeight, 0xFF444444); // Dark gray
			graphics.fill(x, y, x + filled, y + barHeight, 0xFF00AA00); // Green fill
			
			graphics.drawCenteredString(this.font, String.format("%.1f%%", progress * 100), this.width / 2, y + 15, 0xFFFFFF);
		}
	}
}
