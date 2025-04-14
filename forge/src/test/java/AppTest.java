import java.util.List;

import com.supasulley.network.Trie;

class AppTest {
	
	public static void main(String[] args) throws Exception
	{
		Trie t = new Trie(List.of("Hi"));
		
		System.out.println(t.containsAnyIgnoreCase("Hi"));
	}
}
