package io.github.freshsupasulley;

import java.util.List;

public class Transcriptions {
	
	private List<Transcription> transcriptions;
	private int totalRecordings;
	
	public Transcriptions(List<Transcription> transcriptions)
	{
		this.transcriptions = transcriptions;
		this.totalRecordings = transcriptions.stream().mapToInt(Transcription::recordings).sum();
	}
	
	/**
	 * @return true if no transcriptions exist yet
	 */
	public boolean isEmpty()
	{
		return transcriptions.isEmpty();
	}
	
	/**
	 * Assembles all transcriptions into a string.
	 * 
	 * @param separator string used to separate each transcription
	 * @return all transcriptions concatenated as a string
	 */
	public String getRawText(String separator)
	{
		if(transcriptions.isEmpty())
		{
			return "";
		}
		
		StringBuffer buffer = new StringBuffer(transcriptions.getFirst().text());
		
		for(int i = 1; i < transcriptions.size(); i++)
		{
			buffer.append(separator + transcriptions.get(i).text());
		}
		
		return buffer.toString();
	}
	
	/**
	 * Assembles all transcriptions into a string using space as the separator.
	 * 
	 * @return all transcriptions concatenated as a string
	 */
	public String getRawString()
	{
		return getRawText(" ");
	}
	
	/**
	 * @return list of transcriptions
	 */
	public List<Transcription> getTranscriptions()
	{
		return transcriptions;
	}
	
	/**
	 * @return total number of audio recordings processed
	 */
	public int getTotalRecordings()
	{
		return totalRecordings;
	}
	
	/**
	 * Represents a transcription of a variable number of recordings.
	 * 
	 * <p>
	 * When the transcription rate is fast enough to handle the incoming flow of new audio recordings (defined by latency), {@code recordings} will be kept down to
	 * 1. However, if the transcription rate is slower, audio recordings will be concatenated together to form one larger recording. {@code recordings} represents
	 * the number of audio recordings that had to be spliced together.
	 * </p>
	 */
	public record Transcription(String text, int recordings) {
	}
}
