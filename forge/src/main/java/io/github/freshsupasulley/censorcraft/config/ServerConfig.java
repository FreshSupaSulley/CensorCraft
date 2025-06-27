package io.github.freshsupasulley.censorcraft.config;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import com.electronwill.nightconfig.core.serde.annotations.SerdeDefault;
import com.electronwill.nightconfig.core.serde.annotations.SerdeDefault.WhenValue;

import io.github.freshsupasulley.censorcraft.config.punishments.Commands;
import io.github.freshsupasulley.censorcraft.config.punishments.PunishmentOption;
import net.minecraftforge.fml.config.ModConfig;

/**
 * Just know I hate this as much as you do. Only Allah knows why Forge config doesn't support maps (array of tables).
 */
public class ServerConfig extends ConfigFile {
	
	@SerdeDefault(provider = "defaultGlobalTaboos", whenValue = WhenValue.IS_NULL)
	private List<String> GLOBAL_TABOOS = defaultGlobalTaboos.get();
	static transient Supplier<List<String>> defaultGlobalTaboos = () -> Arrays.asList("boom");
	
	@SerdeDefault(provider = "defaultPreferredModel", whenValue = WhenValue.IS_NULL)
	private String PREFERRED_MODEL = defaultPreferredModel.get();
	static transient Supplier<String> defaultPreferredModel = () -> "base.en";
	
	@SerdeDefault(provider = "defaultContextLength", whenValue = WhenValue.IS_NULL)
	private Float CONTEXT_LENGTH = defaultContextLength.get();
	static transient Supplier<Float> defaultContextLength = () -> 3f;
	
	@SerdeDefault(provider = "defaultPunishmentCooldown", whenValue = WhenValue.IS_NULL)
	private Float PUNISHMENT_COOLDOWN = defaultPunishmentCooldown.get();
	static transient Supplier<Float> defaultPunishmentCooldown = () -> 0f;
	
	@SerdeDefault(provider = "defaultChatTaboos", whenValue = WhenValue.IS_NULL)
	private Boolean CHAT_TABOOS = defaultChatTaboos.get();
	static transient Supplier<Boolean> defaultChatTaboos = () -> true;
	
	@SerdeDefault(provider = "defaultExposeRats", whenValue = WhenValue.IS_NULL)
	private Boolean EXPOSE_RATS = defaultExposeRats.get();
	static transient Supplier<Boolean> defaultExposeRats = () -> true;
	
	@SerdeDefault(provider = "defaultIsolateWords", whenValue = WhenValue.IS_NULL)
	private Boolean ISOLATE_WORDS = defaultIsolateWords.get();
	static transient Supplier<Boolean> defaultIsolateWords = () -> true;
	
	@SerdeDefault(provider = "defaultMonitorVoice", whenValue = WhenValue.IS_NULL)
	private Boolean MONITOR_VOICE = defaultMonitorVoice.get();
	static transient Supplier<Boolean> defaultMonitorVoice = () -> true;
	
	@SerdeDefault(provider = "defaultMonitorChat", whenValue = WhenValue.IS_NULL)
	private Boolean MONITOR_CHAT = defaultMonitorChat.get();
	static transient Supplier<Boolean> defaultMonitorChat = () -> true;
	
	@SerdeDefault(provider = "defaultPunishments", whenValue = WhenValue.IS_NULL)
	public PunishmentOption[] PUNISHMENTS = defaultPunishments.get();
	static transient Supplier<PunishmentOption[]> defaultPunishments = () -> new PunishmentOption[] {new Commands()};
	
	public ServerConfig()
	{
		super(ModConfig.Type.SERVER);
		
		// Begin punishments section
		// will be annotatino comment?
		// ConfigWrapper sub = config.sub("punishments", "List of all punishment options. To enable one, set enabled = true", "Each punishment may have their own
		// additional list of taboos that will only trigger that punishment");
		//
		// explosion is enabled by default
		// PUNISHMENTS = new PunishmentOption[] {new Commands()};// , new Crash(), new Dimension(), new Entities(), new Explosion(true), new Ignite(), new Kill(), new
		// Lightning(), new MobEffects(), new Teleport()};
		
		// for(PunishmentOption option : PUNISHMENTS)
		{
			// Build an array of tables with just one table to start. User can add as many
			// as they want
			// sub.buildTable(option.getName(), (table, spec) ->
			// {
			// option.init(table, spec);
			// }, option.getDescription());
			
			// option.init(sub);
		}
	}
	
	public List<String> getGlobalTaboos()
	{
		return config.get("GLOBAL_TABOOS");
	}
	
	public String getPreferredModel()
	{
		return config.get("PREFERRED_MODEL");
	}
	
	public float getContextLength()
	{
		return config.get("CONTEXT_LENGTH");
	}
	
	public float getPunishmentCooldown()
	{
		return config.get("PUNISHMENT_COOLDOWN");
	}
	
	public boolean isChatTaboos()
	{
		return config.get("CHAT_TABOOS");
	}
	
	public boolean isIsolateWords()
	{
		return config.get("ISOLATE_WORDS");
	}
	
	public boolean isMonitorVoice()
	{
		return config.get("MONITOR_VOICE");
	}
	
	public boolean isMonitorChat()
	{
		return config.get("MONITOR_CHAT");
	}
	
	// @Override
	// protected void onConfigUpdate(ModConfig config)
	// {
	// if(!Stream.of(PUNISHMENTS).anyMatch(PunishmentOption::isEnabled))
	// {
	// CensorCraft.LOGGER.warn("No punishments are enabled. Navigate to {} to enable
	// a punishment", config.getFileName());
	// }
	//
	// if(!MONITOR_CHAT.get() && !MONITOR_VOICE.get())
	// {
	// CensorCraft.LOGGER.warn("You are not monitoring voice or chat! CensorCraft is
	// effectively disabled");
	// }
	// }
}