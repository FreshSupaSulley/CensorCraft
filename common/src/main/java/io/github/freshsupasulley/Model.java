package io.github.freshsupasulley;

/**
 * Holds basic information about a Whisper GGML-formatted model that can be downloaded from
 * <a href="https://huggingface.co/api/models/ggerganov/whisper.cpp/tree/main">huggingface</a>.
 * 
 * @param name  model name
 * @param bytes size of model, in bytes
 */
public record Model(String name, long bytes) {
	
	/**
	 * Gets the size of the model as a human-readable string.
	 * 
	 * @see ModelDownloader#getBytesFancy(long)
	 * 
	 * @return human-readable size of model as a string
	 */
	public String getSizeFancy()
	{
		return ModelDownloader.getBytesFancy(bytes);
	}
}