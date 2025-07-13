package forge;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.freshsupasulley.censorcraft.api.punishments.Trie;
import io.github.freshsupasulley.whisperjni.WhisperJNI;

public class SanityTests {
	
	private Trie trie = new Trie(List.of("boom"));
	
	@Test
	public void testAPI()
	{
		System.out.println(WhisperJNI.canUseVulkan());
		String sample = "-ba-ba-ba-boom!-";
		System.out.println(trie.containsAnyIgnoreCase(sample) + " - " + trie.containsAnyIsolatedIgnoreCase(sample));
	}
}
