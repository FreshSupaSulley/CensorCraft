package io.github.freshsupasulley.censorcraft.config.punishments;

import java.util.List;

import io.github.freshsupasulley.censorcraft.config.ServerConfig;
import io.github.freshsupasulley.censorcraft.network.Trie;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

public abstract class PunishmentOption {
	
	private ConfigValue<Boolean> enabled, ignoreGlobalTaboos;
	private ConfigValue<List<? extends String>> taboos;
	private static Trie tabooTree;
	
	private boolean initEnable;
	
	public PunishmentOption(boolean initEnable)
	{
		this.initEnable = initEnable;
	}
	
	public PunishmentOption()
	{
		this(false);
	}
	
	public void init(ForgeConfigSpec.Builder builder)
	{
		String description = getDescription();
		
		if(!description.isBlank())
		{
			builder.comment(description);
		}
		
		builder.push(getName());
		
		this.enabled = builder.define("enable", initEnable);
		this.taboos = builder.comment("List of punishment-specific forbidden words and phrases (case-insensitive)").defineListAllowEmpty("taboo", List.of(), element -> true);
		this.ignoreGlobalTaboos = builder.comment("Global taboos don't trigger this punishment").define("ignore_global_taboos", false);
		build(builder);
		builder.pop();
		
		// Will be updated when getTaboo is called
		tabooTree = new Trie(List.of());
	}
	
	public boolean isEnabled()
	{
		return enabled.get();
	}
	
	public boolean ignoresGlobalTaboos()
	{
		return ignoreGlobalTaboos.get();
	}
	
	public String getTaboo(String word)
	{
		// Update trie in case the taboos did
		tabooTree.update(taboos.get());
		return ServerConfig.ISOLATE_WORDS.get() ? tabooTree.containsAnyIsolatedIgnoreCase(word) : tabooTree.containsAnyIgnoreCase(word);
	}
	
	abstract void build(ForgeConfigSpec.Builder builder);
	public abstract void punish(ServerPlayer player);
	
	public String getName()
	{
		return this.getClass().getSimpleName().toLowerCase();
	}
	
	public String getDescription()
	{
		return "";
	}
}
