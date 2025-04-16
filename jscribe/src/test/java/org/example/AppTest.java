/*
 * This source file was generated by the Gradle 'init' task
 */
package org.example;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import com.supasulley.jscribe.JScribe;

class AppTest {
	
	/**
	 * This doesn't work when running from the IDE on Mac. Thanks Tim Apple!
	 */
	@Test
	void testModel()
	{
		JScribe scribe = new JScribe(Paths.get("src/test/resources/ggml-tiny.en.bin"));
		scribe.start("", 1000, 500);
		
		// Translate for a while
		long start = System.currentTimeMillis(), lastAudio = start;
		
		while(System.currentTimeMillis() - start < 30000 && !scribe.noAudio())
		{
			if(System.currentTimeMillis() - lastAudio > 200)
			{
				lastAudio = System.currentTimeMillis();
				System.out.println("Audio level: " + scribe.getAudioLevel());
			}
			
			for(String buffer = null; !(buffer = scribe.getBuffer()).equals(""); System.out.println(buffer));
		}
	}
}
