package io.github.freshsupasulley.censorcraft.plugins.impl.server;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import io.github.freshsupasulley.censorcraft.api.CensorCraftServerAPI;
import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;
import io.github.freshsupasulley.censorcraft.network.WordPacket;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CensorCraftServerAPIImpl implements CensorCraftServerAPI {
	
	// Instance everyone can see
	public static CensorCraftServerAPI INSTANCE;
	
	// Begin instance variables
	private final CommentedFileConfig config;
	
	public CensorCraftServerAPIImpl(CommentedFileConfig config)
	{
		this.config = config;
	}
	
	@Override
	public void punish(de.maxhenkel.voicechat.api.ServerPlayer player, @Nullable String taboo, Punishment... punishments)
	{
		WordPacket.punish((ServerPlayer) player.getPlayer(), taboo, List.of(punishments));
	}
	
	public CommentedFileConfig getServerConfig()
	{
		return config;
	}
}
