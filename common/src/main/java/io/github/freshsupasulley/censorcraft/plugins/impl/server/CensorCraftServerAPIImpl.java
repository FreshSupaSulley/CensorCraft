package io.github.freshsupasulley.censorcraft.plugins.impl.server;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import io.github.freshsupasulley.censorcraft.api.CensorCraftServerAPI;
import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;
import io.github.freshsupasulley.censorcraft.config.ServerConfig;
import io.github.freshsupasulley.censorcraft.network.WordPacket;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @param config Begin instance variables
 */
public record CensorCraftServerAPIImpl(CommentedFileConfig config) implements CensorCraftServerAPI {
	
	// Instance everyone can see
	public static CensorCraftServerAPI INSTANCE;
	
	@Override
	public void punish(Object player, @Nullable String taboo, Punishment... punishments)
	{
		WordPacket.punish((ServerPlayer) player, taboo, List.of(punishments));
	}
	
	@Override
	public List<Punishment> getConfigPunishments()
	{
		return ServerConfig.get().getConfigPunishments();
	}
	
	public CommentedFileConfig getServerConfig()
	{
		return config;
	}
}
