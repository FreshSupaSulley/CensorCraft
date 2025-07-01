package io.github.freshsupasulley.censorcraft.config.punishments;

import java.util.ArrayList;
import java.util.List;

import com.electronwill.nightconfig.core.CommentedConfig;

import io.github.freshsupasulley.censorcraft.network.Trie;
import net.minecraft.server.level.ServerPlayer;

public abstract class PunishmentOption<T extends PunishmentOption<T>> {
	
	// private ConfigValue<Boolean> enabled, ignoreGlobalTaboos;
//	@SerdeDefault(provider = "defaultTaboos", whenValue = WhenValue.IS_NULL)
//	private List<String> taboos;
//	public static transient Supplier<List<String>> defaultTaboos = () -> Arrays.asList("boom");
	
	protected CommentedConfig config;
	
	private Trie tabooTrie;
	
	public void init(boolean initEnable, CommentedConfig config)
	{
		config.set("enable", initEnable);
		
		config.set("taboo", new ArrayList<>(List.of("")));
		config.setComment("taboo", "List of punishment-specific forbidden words and phrases (case-insensitive)");
		tabooTrie = new Trie(List.of()); // Will be updated when getTaboo is called
		
		config.set("ignore_global_taboos", false);
		config.setComment("ignore_global_taboos", "Global taboos don't trigger this punishment");
		
		build(config);
	}
	
	abstract void build(CommentedConfig config);
	abstract T newInstance();
	
	public T deserialize(CommentedConfig config)
	{
		this.config = config;
		return newInstance();
	}
	
	public boolean isEnabled()
	{
		return config.get("enable");
	}
	
	public boolean ignoresGlobalTaboos()
	{
		return config.get("ignore_global_taboos");
	}
	
	public String getTaboo(String word, boolean isolateWords)
	{
		// Update trie in case the taboos did
		tabooTrie.update(config.get("taboo"));
		return isolateWords ? tabooTrie.containsAnyIsolatedIgnoreCase(word) : tabooTrie.containsAnyIgnoreCase(word);
	}
	
	public abstract void punish(ServerPlayer player);
	
	public String getName()
	{
		return this.getClass().getSimpleName().toLowerCase();
	}
	
	public String[] getDescription()
	{
		return new String[0];
	}
}
