package io.github.freshsupasulley.censorcraft.api.punishments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Represents a special data structure to efficiently find strings in a tree-like manner.
 */
public class Trie {
	
	private List<String> list;
	private TrieNode root;
	
	public Trie(Iterable<?> rawList)
	{
		update(rawList);
	}
	
	public void update(Iterable<?> rawList)
	{
		// Check if we need to update
		List<String> newList = StreamSupport.stream(rawList.spliterator(), false).map(Object::toString).collect(Collectors.toList());
		
		if(!newList.equals(list))
		{
			list = new ArrayList<String>();
			root = new TrieNode();
			newList.forEach(item -> insert(item));
		}
	}
	
	private void insert(String word)
	{
		word = word.toLowerCase();
		list.add(word);
		
		TrieNode node = root;
		
		for(char c : word.toCharArray())
		{
			node = node.children.computeIfAbsent(c, k -> new TrieNode());
		}
		
		node.isEndOfWord = true;
	}
	
	/**
	 * Returns the first word found in the text (case-insensitive), or null if none was found.
	 * 
	 * @param text text to check
	 * @return first word found, or null
	 */
	public String containsAnyIgnoreCase(String text)
	{
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
					return foundWord.toString();
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Returns the first full word found in the text (case-insensitive), or null if none was found.
	 * 
	 * @param text text to check
	 * @return first word found, or null
	 */
	public String containsAnyIsolatedIgnoreCase(String text)
	{
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
						return foundWord.toString();
					}
				}
			}
		}
		
		return null;
	}
	
	private static class TrieNode {
		
		Map<Character, TrieNode> children = new HashMap<>();
		boolean isEndOfWord = false;
	}
}
