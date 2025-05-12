package com.supasulley.censorcraft.gui;

import java.io.IOException;

import com.supasulley.censorcraft.CensorCraft;
import com.supasulley.censorcraft.ClientCensorCraft;

import io.github.freshsupasulley.JScribe;
import io.github.freshsupasulley.Model;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public class DownloadScreen extends Screen {
	
	private long downloaded;
	private boolean complete;
	
	private Model model;
	
	public DownloadScreen(String modelName)
	{
		super(Component.literal("Downloading ").append(Component.literal(modelName).withStyle(Style.EMPTY.withBold(true))));
		
		Thread thread = new Thread(() ->
		{
			try
			{
				// Verification that the server is requesting a model that actually exists on hugging face
				model = JScribe.getModelInfo(modelName);
				
				if(model == null)
				{
					minecraft.execute(() -> minecraft.setScreen(new DisconnectedScreen(null, Component.literal("Server requested a model that doesn't exist"), Component.literal("Ask the server owner to fix their config"))));
				}
				else
				{
					// Update title to include size
					JScribe.downloadModel(modelName, ClientCensorCraft.getModelPath(modelName), (downloaded, total) ->
					{
						this.downloaded = downloaded;
					});
					
					complete = true;
					minecraft.execute(() -> minecraft.setScreen(new PopupScreen.Builder(this, Component.literal("Downloaded model")).setMessage(Component.literal("Reconnect to join. You can manage downloaded models in the mod config menu.")).addButton(CommonComponents.GUI_OK, (screen) -> Minecraft.getInstance().setScreen(null)).build()));
				}
			} catch(IOException e)
			{
				CensorCraft.LOGGER.error("Something went wrong downloading model {}", modelName, e);
				minecraft.execute(() -> minecraft.setScreen(new DisconnectedScreen(null, Component.literal("An error occurred downloading the model"), Component.literal(e.getMessage()))));
			}
		});
		
		thread.setDaemon(true);
		thread.setName("CensorCraft Model Downloader");
		thread.start();
	}
	
	@Override
	public void render(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick)
	{
		super.render(graphics, pMouseX, pMouseY, pPartialTick);
		
		if(model == null)
		{
			graphics.drawCenteredString(font, Component.literal("Validating..."), this.width / 2, this.height / 2, 0xFFFFFFFF);
		}
		// Don't draw the progress bar when done (it shows underneath the popup and looks ugly asf)
		else if(!complete)
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
