package com.supasulley.censorcraft;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.supasulley.censorcraft.network.Trie;

class SanityTests {
	
	@Test
	void testTrie()
	{
		final boolean isolateWords = true;
		
		Trie trie = new Trie(List.of("art"));
		
		String word = "shart";
		System.out.println("WORDS:: " + (isolateWords ? trie.containsAnyIsolatedIgnoreCase(word) : trie.containsAnyIgnoreCase(word)));
	}
}
