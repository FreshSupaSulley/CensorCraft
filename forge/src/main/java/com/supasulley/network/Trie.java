package com.supasulley.network;

import java.util.*;

public class Trie {
	
	private List<String> list;
	private final TrieNode root = new TrieNode();
	
	public Trie(Iterable<?> rawList)
	{
		this.list = new ArrayList<String>();
		rawList.forEach(item -> insert(item.toString()));
	}
	
	public List<String> getWords()
	{
		return list;
	}
	
	public void insert(String word)
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
	
	private static class TrieNode {
		
		Map<Character, TrieNode> children = new HashMap<>();
		boolean isEndOfWord = false;
	}
}
