package io.github.freshsupasulley.censorcraft.api.punishments;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

/**
 * Represents a special data structure to efficiently find strings in a tree-like manner.
 */
public class Trie {
	
	private final TrieNode root;
	
	/**
	 * Defines a new Trie with a list of initial entries that will be <code>toString()</code>'d and inserted into the
	 * Trie.
	 *
	 * @param rawList entries
	 */
	public Trie(Iterable<?> rawList)
	{
		root = new TrieNode();
		
		// Add each toString()'d object to the trie
		StreamSupport.stream(rawList.spliterator(), false).map(Object::toString).toList().forEach(word ->
		{
			word = word.toLowerCase();
			
			TrieNode node = root;
			
			for(char c : word.toCharArray())
			{
				node = node.children.computeIfAbsent(c, k -> new TrieNode());
			}
			
			node.isEndOfWord = true;
		});
	}
	
	/**
	 * Returns the words found in the text (case-insensitive), or null if none was found. Word boundaries are not
	 * considered.
	 *
	 * @param text text to check
	 * @return words found, or null
	 */
	public @Nullable Set<String> findEntries(String text)
	{
		Set<String> result = new LinkedHashSet<>();
		
		for(int i = 0; i < text.length(); i++)
		{
			TrieNode node = root;
			StringBuilder foundWord = new StringBuilder();
			
			for(int j = i; j < text.length(); j++)
			{
				char c = Character.toLowerCase(text.charAt(j));
				if(!node.children.containsKey(c))
					break;
				
				node = node.children.get(c);
				foundWord.append(c);
				
				if(node.isEndOfWord)
				{
					result.add(foundWord.toString());
				}
			}
		}
		
		return result.isEmpty() ? null : result;
	}
	
	/**
	 * Returns <b>full words</b> (words separated by whitespace, non-letters, or end of line) found in the text
	 * (case-insensitive), or null if none was found.
	 *
	 * @param text text to check
	 * @return words found, or null
	 */
	public @Nullable Set<String> findIsolatedEntries(String text)
	{
		// this removes dupes
		Set<String> result = new LinkedHashSet<>();
		
		for(int i = 0; i < text.length(); i++)
		{
			TrieNode node = root;
			StringBuilder foundWord = new StringBuilder();
			
			for(int j = i; j < text.length(); j++)
			{
				char c = Character.toLowerCase(text.charAt(j));
				if(!node.children.containsKey(c))
					break;
				
				node = node.children.get(c);
				foundWord.append(c);
				
				if(node.isEndOfWord)
				{
					// Check word boundaries
					boolean validPrefix = (i == 0 || !Character.isLetter(text.charAt(i - 1)));
					boolean validSuffix = (j + 1 == text.length() || !Character.isLetter(text.charAt(j + 1)));
					
					if(validPrefix && validSuffix)
					{
						result.add(foundWord.toString());
					}
				}
			}
		}
		
		return result.isEmpty() ? null : result;
	}
	
	private static class TrieNode {
		
		Map<Character, TrieNode> children = new HashMap<>();
		boolean isEndOfWord = false;
	}
}
