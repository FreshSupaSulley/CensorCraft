package forge;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import io.github.freshsupasulley.censorcraft.network.Trie;
import io.github.freshsupasulley.whisperjni.LibraryUtils;

public class SanityTests {
	
	private Trie trie = new Trie(List.of("boom"));
	
	@Test
	public void testAPI()
	{
		System.out.println(LibraryUtils.canUseVulkan(CensorCraft.LOGGER));
		String sample = "-ba-ba-ba-boom!-";
		System.out.println(trie.containsAnyIgnoreCase(sample) + " - " + trie.containsAnyIsolatedIgnoreCase(sample));
	}
}
