package forge;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.freshsupasulley.censorcraft.api.punishments.Trie;

public class SanityTests {
	
	private Trie trie = new Trie(List.of("boom"));
	
	@Test
	public void testAPI()
	{
		String sample = "-ba-ba-ba-boom!-";
		System.out.println(trie.containsAnyIgnoreCase(sample) + " - " + trie.containsAnyIsolatedIgnoreCase(sample));
	}
}
