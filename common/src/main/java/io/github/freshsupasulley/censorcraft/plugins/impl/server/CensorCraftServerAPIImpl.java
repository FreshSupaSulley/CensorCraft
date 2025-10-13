package io.github.freshsupasulley.censorcraft.plugins.impl.server;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import io.github.freshsupasulley.censorcraft.api.CensorCraftServerAPI;
import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;
import io.github.freshsupasulley.censorcraft.config.ServerConfig;
import io.github.freshsupasulley.censorcraft.network.WordPacket;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @param config Begin instance variables
 */
public record CensorCraftServerAPIImpl(CommentedFileConfig config) implements CensorCraftServerAPI {
	
	// Instance everyone can see
	public static CensorCraftServerAPI INSTANCE;
	
	@Override
	public void punish(Object player, Map<Punishment, @Nullable String> punishments)
	{
		WordPacket.punish((ServerPlayer) player, punishments);
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
