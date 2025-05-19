package io.github.freshsupasulley.censorcraft.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import io.github.freshsupasulley.JScribe;
import io.github.freshsupasulley.Model;
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
	
	private CompletableFuture<Void> downloadTask;
	private Path downloadPath;
	
	private long downloaded;
	
	private Model model;
	
	public DownloadScreen(Model model)
	{
		super(Component.literal("Downloading ").append(Component.literal(model.name()).withStyle(Style.EMPTY.withBold(true))));
		
		this.model = model;
		
		// Update title to include size
		downloadTask = JScribe.downloadModel(model.name(), downloadPath = ClientCensorCraft.getModelPath(model.name()), (downloaded, total) ->
		{
			this.downloaded = downloaded;
		}).whenComplete((success, error) ->
		{
			// On success
			if(error == null)
			{
				minecraft.execute(() -> minecraft.setScreen(new PopupScreen.Builder(new TitleScreen(), Component.literal("Downloaded model")).setMessage(Component.literal("Reconnect to join. You can manage downloaded models in the mod config menu.")).addButton(CommonComponents.GUI_OK, PopupScreen::onClose).build()));
			}
			// On error
			else
			{
				this.onClose(); // delete the incomplete model?
				minecraft.execute(() -> minecraft.setScreen(ClientCensorCraft.errorScreen("An error occurred downloading the model", error)));
			}
		});
	}
	
	@Override
	public void onClose()
	{
		super.onClose();
		
		CensorCraft.LOGGER.info("{} model download cancelled", model.name());
		
		try
		{
			if(Files.deleteIfExists(downloadPath))
			{
				CensorCraft.LOGGER.warn("Deleted incomplete model at {}", downloadPath);
			}
			else
			{
				CensorCraft.LOGGER.warn("Incomplete model at {} does not exist", downloadPath);
			}
		} catch(IOException e)
		{
			CensorCraft.LOGGER.error("Failed to delete incomplete model download at {}", downloadPath, e);
		}
		
		downloadTask.cancel(true);
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
		
		// Don't draw the progress bar when done (it shows underneath the popup and looks ugly asf)
		if(!downloadTask.isDone())
		{
			// Cutoff the byte suffix
			graphics.drawCenteredString(font, title, this.width / 2, this.height / 4, 0xFFFFFFFF);
			graphics.drawCenteredString(font, Component.literal(Model.getBytesFancy(downloaded) + " / " + model.getSizeFancy()), this.width / 2, this.height / 4 + font.lineHeight * 2, 0xFFFFFFFF);
			
			final int barWidth = 200;
			final int barHeight = 10;
			
			int x = (this.width - barWidth) / 2;
			int y = this.height / 2;
			
			final float progress = downloaded * 1f / model.bytes();
			
			int filled = (int) (barWidth * progress);
			
			graphics.fill(x, y, x + barWidth, y + barHeight, 0xFF444444); // Dark gray
			graphics.fill(x, y, x + filled, y + barHeight, 0xFF00AA00); // Green fill
			
			graphics.drawCenteredString(this.font, String.format("%.1f%%", progress * 100), this.width / 2, y + 15, 0xFFFFFF);
		}
	}
}
