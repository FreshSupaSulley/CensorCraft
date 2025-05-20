package io.github.freshsupasulley;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;

public class ModelDownloader {
	
	private volatile boolean cancelled, done;
	
	private long bytesRead, downloadSize;
	
	ModelDownloader(String modelName, Path destination)
	{
		String fileName = "ggml-" + modelName + ".bin";
		String downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/" + fileName;
		
		JScribe.logger.info("Downloading from {}", downloadUrl);
		
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(downloadUrl)).build();
		// It sends you to a unique node so follow the redirect
		HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
		
		client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream()).thenAcceptAsync((response) ->
		{
			downloadSize = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
			
			if(response.statusCode() != 200)
			{
				throw new CompletionException(new IOException("Failed to download model. HTTP status code: " + response.statusCode()));
			}
			
			try(InputStream in = response.body(); FileOutputStream out = new FileOutputStream(destination.toFile()))
			{
				byte[] buffer = new byte[8192];
				int read;
				
				// Wrap the InputStream to track the download progress
				while(!cancelled && (read = in.read(buffer)) != -1)
				{
					out.write(buffer, 0, read);
					bytesRead += read;
				}
				
				if(cancelled)
				{
					throw new CancellationException("Download cancelled");
				}
				
				JScribe.logger.info("Downloaded {} to {}", modelName, destination);
			} catch(IOException e)
			{
				throw new CompletionException(e);
			}
		}).whenComplete((result, exception) ->
		{
			done = true;
			
			// If a problem occurred
			if(exception != null)
			{
				JScribe.logger.warn("Download failed", exception);
				
				try
				{
					if(Files.deleteIfExists(destination))
					{
						JScribe.logger.warn("Deleted incomplete model at {}", destination);
					}
					else
					{
						JScribe.logger.warn("Incomplete model at {} does not exist", destination);
					}
				} catch(IOException e)
				{
					JScribe.logger.error("Failed to delete incomplete model download at {}", destination, e);
				}
			}
		});
	}
	
	/**
	 * Signals to stop the download task.
	 */
	public void cancel()
	{
		this.cancelled = true;
	}
	
	/**
	 * Returns true if completed normally or exceptionally.
	 * 
	 * @return true if finished, false if still running
	 */
	public boolean isDone()
	{
		return done;
	}
	
	/**
	 * Gets the bytes read so far.
	 * 
	 * @return number of bytes read
	 */
	public long getBytesRead()
	{
		return bytesRead;
	}
	
	/**
	 * Gets the total download size of the model.
	 * 
	 * @return size of the model, in bytes
	 */
	public long getDownloadSize()
	{
		return downloadSize;
	}
}
