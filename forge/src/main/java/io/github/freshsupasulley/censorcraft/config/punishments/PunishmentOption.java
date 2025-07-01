package io.github.freshsupasulley.censorcraft.config.punishments;

import java.util.List;

import com.electronwill.nightconfig.core.CommentedConfig;

import io.github.freshsupasulley.censorcraft.network.Trie;
import net.minecraft.server.level.ServerPlayer;

public abstract class PunishmentOption {
	
	// private ConfigValue<Boolean> enabled, ignoreGlobalTaboos;
//	@SerdeDefault(provider = "defaultTaboos", whenValue = WhenValue.IS_NULL)
//	private List<String> taboos;
//	public static transient Supplier<List<String>> defaultTaboos = () -> Arrays.asList("boom");
	
	protected CommentedConfig config;
	
	private boolean enable;
	private Trie tabooTrie;
	private boolean ignoreGlobalTaboos;
	
	public PunishmentOption(CommentedConfig config)
	{
		this.config = config;
	}
	
	public void init(boolean initEnable, CommentedConfig config)
	{
		config.set("enable", initEnable);
		
		config.set("taboo", List.of(""));
		config.setComment("taboo", "List of punishment-specific forbidden words and phrases (case-insensitive)");
		// Will be updated when getTaboo is called
		tabooTrie = new Trie(List.of());
		
		config.set("ignore_global_taboos", false);
		config.setComment("ignore_global_taboos", "Global taboos don't trigger this punishment");
		
		build(config);
	}
	
	abstract void build(CommentedConfig config);
	public abstract PunishmentOption deserialize(CommentedConfig config2);
	
	public boolean isEnabled()
	{
		return enable;
	}
	
	public boolean ignoresGlobalTaboos()
	{
		return ignoreGlobalTaboos;
	}
	
	public String getTaboo(String word, boolean isolateWords)
	{
		// Update trie in case the taboos did
		tabooTrie.update(config.get("taboos"));
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
