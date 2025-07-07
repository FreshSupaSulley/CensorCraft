package io.github.freshsupasulley;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.freshsupasulley.Transcriptions.Transcription;
import io.github.givimad.whisperjni.TokenData;

/**
 * Represents a collection of {@link Transcription} records.
 */
public class Transcriptions implements Iterable<Transcription> {
	
	private List<Transcription> transcriptions;
	private int totalRecordings;
	
	Transcriptions(List<Transcription> transcriptions)
	{
		this.transcriptions = transcriptions;
		this.totalRecordings = transcriptions.stream().mapToInt(Transcription::recordings).sum();
	}
	
	/**
	 * Returns true if this doesn't have any transcriptions.
	 * 
	 * @return true if no transcriptions exist yet
	 */
	public boolean isEmpty()
	{
		return transcriptions.isEmpty();
	}
	
	/**
	 * Returns the array of transcriptions composing this collection.
	 * 
	 * @return list of transcriptions
	 */
	public List<Transcription> getTranscriptions()
	{
		return transcriptions;
	}
	
	/**
	 * Returns the sum of all audio recordings processed across each {@link Transcription}.
	 * 
	 * @return number of audio recordings processed
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
	 * 
	 * @param tokens         raw tokens received from the model
	 * @param recordings     amount of recordings concatenated together that compose this transcription
	 * @param processingTime time it took to process these recordings, in milliseconds
	 */
	public static record Transcription(TokenData[] tokens, int recordings, long processingTime) {
		
		/**
		 * Lazily assembles all tokens into a string.
		 * 
		 * @return combined string of tokens
		 */
		public String text()
		{
			return Stream.of(tokens).map(token -> token.token).collect(Collectors.joining());
		}
	}
	
	@Override
	public Iterator<Transcription> iterator()
	{
		return transcriptions.iterator();
	}
}
