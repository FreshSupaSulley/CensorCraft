package io.github.freshsupasulley.censorcraft.config;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import com.electronwill.nightconfig.core.serde.annotations.SerdeAssert;
import com.electronwill.nightconfig.core.serde.annotations.SerdeDefault;
import com.electronwill.nightconfig.core.serde.annotations.SerdeDefault.WhenValue;

import io.github.freshsupasulley.censorcraft.config.punishments.Commands;
import io.github.freshsupasulley.censorcraft.config.punishments.PunishmentOption;

public class ServerConfig extends ConfigFile {
	
	@SerdeDefault(provider = "globalTaboo", whenValue = WhenValue.IS_NULL)
	public List<String> GLOBAL_TABOO;
	static transient Supplier<List<String>> globalTaboo = () -> Arrays.asList("fart");

	@SerdeDefault(provider = "defaultServers", whenValue = WhenValue.IS_NULL)
	public String PREFERRED_MODEL;

	public static String defaultServers() {
		return "boom";
	}

	public float contextLength;
	public float punishmentCooldown;
	public boolean CHAT_TABOOS, /* EXPOSE_RATS, */ISOLATE_WORDS, MONITOR_VOICE, MONITOR_CHAT;

	@SerdeDefault(provider = "defaultPunishments", whenValue = WhenValue.IS_NULL)
	public PunishmentOption[] PUNISHMENTS;

	public static PunishmentOption[] defaultPunishments() {
		return new PunishmentOption[] { new Commands() };
	}

	public ServerConfig() {
		super(new File("server.toml"));

		// Begin punishments section
		// will be annotatino comment?
//		ConfigWrapper sub = config.sub("punishments", "List of all punishment options. To enable one, set enabled = true", "Each punishment may have their own additional list of taboos that will only trigger that punishment");
//		
		// explosion is enabled by default
//		PUNISHMENTS = new PunishmentOption[] {new Commands()};// , new Crash(), new Dimension(), new Entities(), new Explosion(true), new Ignite(), new Kill(), new Lightning(), new MobEffects(), new Teleport()};

//		for(PunishmentOption option : PUNISHMENTS)
		{
			// Build an array of tables with just one table to start. User can add as many
			// as they want
//			sub.buildTable(option.getName(), (table, spec) ->
//			{
//				option.init(table, spec);
//			}, option.getDescription());

			// option.init(sub);
		}
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