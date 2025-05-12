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
	 * @see Model#getBytesFancy(long)
	 * 
	 * @return human-readable size of model as a string
	 */
	public String getSizeFancy()
	{
		return getBytesFancy(bytes);
	}
	
	/**
	 * Converts number of bytes to a human-readable string, such as "10 GB".
	 * 
	 * @param bytes
	 * @return human-readable size of model as a string
	 */
	public static String getBytesFancy(long bytes)
	{
		// https://gist.github.com/markuswustenberg/1370480 (goated)
		int unit = 1024;
		if(bytes < unit)
			return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		return String.format("%.1f %sB", bytes / Math.pow(unit, exp), "KMGTPE".charAt(exp - 1));
	}
}