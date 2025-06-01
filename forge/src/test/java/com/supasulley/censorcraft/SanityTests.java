package com.supasulley.censorcraft;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.github.freshsupasulley.JScribe;
import io.github.freshsupasulley.RollingAudioBuffer;
import io.github.freshsupasulley.Transcriptions;
import io.github.freshsupasulley.censorcraft.network.Trie;

class SanityTests {
	
	@Test
	void testTrie()
	{
		final boolean isolateWords = true;
		
		Trie trie = new Trie(List.of("art"));
		
		String word = "shart";
		System.out.println("WORDS:: " + (isolateWords ? trie.containsAnyIsolatedIgnoreCase(word) : trie.containsAnyIgnoreCase(word)));
	}
	
	@Disabled
	@Test
	void testRAB() throws Exception
	{
		final int SAMPLE_RATE = 48000;
		final RollingAudioBuffer ringBuffer = new RollingAudioBuffer(30000, SAMPLE_RATE);
		ringBuffer.append(new short[SAMPLE_RATE * 29]);
		
		File temp = File.createTempFile("temp", "file");
		temp.deleteOnExit();
		
		AtomicBoolean complete = new AtomicBoolean();
		JScribe.downloadModel("tiny.en", temp.toPath(), (one, two) ->
		{
			complete.set(true);
		});
		
		while(!complete.get())
		{
			System.out.println("Waiting");
			Thread.sleep(1000);
		}
		
		JScribe scribe = new JScribe.Builder(temp.toPath()).build();
		scribe.start();
		
		float[] samples = ringBuffer.getSnapshot();
		
		System.out.println((samples.length * 1000f) / 16000 + " ms");
		
		scribe.transcribe(samples);
		
		Transcriptions t = null;
		
		while((t = scribe.getTranscriptions()) == null)
		{
			System.out.println("Waiting on transcription");
			Thread.sleep(1000);
		}
		
		System.out.println("GOT IT" + t.getRawString());
		
		// Thread thread = new Thread(() ->
		// {
		//
		// });
	}
}
