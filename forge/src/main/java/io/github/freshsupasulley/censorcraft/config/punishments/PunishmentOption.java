package io.github.freshsupasulley.censorcraft.config.punishments;

import java.util.List;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigSpec;

import io.github.freshsupasulley.censorcraft.config.ServerConfig;
import io.github.freshsupasulley.censorcraft.network.Trie;
import net.minecraft.server.level.ServerPlayer;

public abstract class PunishmentOption {
	
	// private ConfigValue<Boolean> enabled, ignoreGlobalTaboos;
	// private ConfigValue<List<String>> taboos;
	protected CommentedConfig config;
	private Trie tabooTree;
	
	private boolean initEnable;
	
	public PunishmentOption(boolean initEnable)
	{
		this.initEnable = initEnable;
	}
	
	public PunishmentOption()
	{
		this(false);
	}
	
	public void init(CommentedConfig config, ConfigSpec spec)
	{
		this.config = config;
		
		spec.define("enable", initEnable);
		
		spec.define("taboo", List.of(""));
		config.setComment("taboo", "List of punishment-specific forbidden words and phrases (case-insensitive)");
		
		spec.define("ignore_global_taboos", false);
		config.setComment("ignore_global_taboos", "Global taboos don't trigger this punishment");
		
		build(config, spec);
		
		// Will be updated when getTaboo is called
		tabooTree = new Trie(List.of());
	}
	
	abstract void build(CommentedConfig config, ConfigSpec spec);
	
	public boolean isEnabled()
	{
		return config.get("enabled");
	}
	
	public boolean ignoresGlobalTaboos()
	{
		return config.get("ignore_global_taboos");
	}
	
	public String getTaboo(String word)
	{
		// Update trie in case the taboos did
		tabooTree.update(config.get("taboo"));
		return ServerConfig.ISOLATE_WORDS.get() ? tabooTree.containsAnyIsolatedIgnoreCase(word) : tabooTree.containsAnyIgnoreCase(word);
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
