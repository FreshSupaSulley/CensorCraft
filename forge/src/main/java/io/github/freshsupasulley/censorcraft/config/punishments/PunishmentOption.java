package io.github.freshsupasulley.censorcraft.config.punishments;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import com.electronwill.nightconfig.core.serde.annotations.SerdeDefault;
import com.electronwill.nightconfig.core.serde.annotations.SerdeDefault.WhenValue;

import io.github.freshsupasulley.censorcraft.network.Trie;

public abstract class PunishmentOption {
	
	// private ConfigValue<Boolean> enabled, ignoreGlobalTaboos;
//	@SerdeDefault(provider = "defaultTaboos", whenValue = WhenValue.IS_NULL)
//	private List<String> taboos;
//	public static transient Supplier<List<String>> defaultTaboos = () -> Arrays.asList("boom");
	
	private boolean enable;
	private boolean ignoreGlobalTaboos;
	private transient Trie tabooTree;
	
	public void init()
	{
//		this.config = config;
//		
//		spec.define("enable", initEnable);
//		
//		spec.define("taboo", List.of(""));
//		config.setComment("taboo", "List of punishment-specific forbidden words and phrases (case-insensitive)");
//		
//		spec.define("ignore_global_taboos", false);
//		config.setComment("ignore_global_taboos", "Global taboos don't trigger this punishment");
//		
//		build(config, spec);
//		
//		// Will be updated when getTaboo is called
//		tabooTree = new Trie(List.of());
	}
	
//	abstract void build(CommentedConfig config, ConfigSpec spec);
	
	public boolean isEnabled()
	{
		return enable;
	}
	
	public boolean ignoresGlobalTaboos()
	{
		return ignoreGlobalTaboos;
	}
	
	public String getTaboo(String word)
	{
		// Update trie in case the taboos did
//		tabooTree.update(taboos);
		return "";
	}
	
	public abstract void punish();
	
	public String getName()
	{
		return this.getClass().getSimpleName().toLowerCase();
	}
	
	public String[] getDescription()
	{
		return new String[0];
	}
}
