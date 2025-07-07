package forge;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.freshsupasulley.censorcraft.network.Trie;
import io.github.givimad.whisperjni.internal.LibraryUtils;

public class TestWhisperJNI {
	
	private Trie trie = new Trie(List.of("boom"));
	
	@Test
	public void testAPI()
	{
		System.out.println(LibraryUtils.canUseVulkan());
		String sample = "-ba-ba-ba-boom!-";
		System.out.println(trie.containsAnyIgnoreCase(sample) + " - " + trie.containsAnyIsolatedIgnoreCase(sample));
	}
}
