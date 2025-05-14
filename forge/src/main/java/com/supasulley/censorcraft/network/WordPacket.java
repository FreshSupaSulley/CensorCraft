package com.supasulley.censorcraft.network;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import com.supasulley.censorcraft.CensorCraft;
import com.supasulley.censorcraft.Config;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = CensorCraft.MODID)
public class WordPacket implements IPacket {
	
	/** Only used server side! */
	private static Trie tabooTree;
	private static Map<UUID, Participant> participants;
	private static long lastSystemRat;
	
	private String payload;
	
	public WordPacket(String payload)
	{
		this.payload = payload;
	}
	
	public WordPacket(FriendlyByteBuf buffer)
	{
		this.payload = buffer.readCharSequence(buffer.readInt(), Charset.defaultCharset()).toString();
	}
	
	@Override
	public void encode(FriendlyByteBuf buffer)
	{
		byte[] bytes = payload.getBytes(Charset.defaultCharset());
		buffer.writeInt(bytes.length);
		buffer.writeBytes(bytes);
	}
	
	@Override
	public void consume(CustomPayloadEvent.Context context)
	{
		consume(context.getSender());
	}
	
	public void consume(ServerPlayer player)
	{
		Participant participant = participants.get(player.getUUID());
		
		// Put into heartbeat map
		participant.heartbeat();
		
		// Just a heartbeat, ignore
		if(payload.isBlank())
		{
			CensorCraft.LOGGER.debug("Received heartbeat from {}", player.getUUID());
			return;
		}
		
		CensorCraft.LOGGER.info("Received \"{}\" from {}", payload, participant.getName());
		
		// Update trie in case the taboos did
		tabooTree.update(Config.Server.TABOO.get());
		
		String word = participant.appendWord(payload);
		String taboo = Config.Server.ISOLATE_WORDS.get() ? tabooTree.containsAnyIsolatedIgnoreCase(word) : tabooTree.containsAnyIgnoreCase(word);
		
		// If we didn't find anything
		if(taboo == null)
			return;
		
		CensorCraft.LOGGER.info("Taboo said by {}: \"{}\"!", participant.getName(), taboo);
		participant.clearBuffer();
		
		// If we need to wait before the player is punished again
		long lastPunishmentTime = System.currentTimeMillis() - participant.getLastPunishment();
		
		if(lastPunishmentTime < Config.Server.PUNISHMENT_COOLDOWN.get() * 1000) // Convert taboo cooldown to ms
		{
			CensorCraft.LOGGER.info("Can't punish {} this frequently (last punishment was {}ms ago)", participant.getName(), lastPunishmentTime);
			return;
		}
		
		CensorCraft.LOGGER.info("Punishing {}", participant.getName());
		
		// Update punishment timing and clear buffer
		participant.updateLastPunishment();
		
		// Notify all players of the sin
		if(Config.Server.CHAT_TABOOS.get())
		{
			player.level().players().forEach(sample -> sample.displayClientMessage(Component.literal(participant.getName()).withStyle(style -> style.withBold(true)).append(Component.literal(" said ").withStyle(style -> style.withBold(false))).append(Component.literal("\"" + taboo + "\"")), false));
		}
		
		// Kill the player
		if(!player.isDeadOrDying())
		{
			punish(player);
		}
	}
	
	@SubscribeEvent
	public static void serverSetup(ServerStartingEvent event)
	{
		CensorCraft.LOGGER.info("Loading taboo tree");
		tabooTree = new Trie(Config.Server.TABOO.get());
		participants = new HashMap<UUID, Participant>();
		lastSystemRat = System.currentTimeMillis();
	}
	
	// Server side only apparently
	@SubscribeEvent
	public static void playerJoinedEvent(PlayerLoggedInEvent event)
	{
		if(event.getEntity() instanceof ServerPlayer player)
		{
			CensorCraft.LOGGER.info("{} joined the server", player.getScoreboardName());
			participants.put(player.getUUID(), new Participant(player.getScoreboardName()));
		}
	}
	
	// Server side only apparently
	@SubscribeEvent
	public static void playerLeftEvent(PlayerLoggedOutEvent event)
	{
		if(event.getEntity() instanceof ServerPlayer player)
		{
			CensorCraft.LOGGER.info("{} left the server", player.getScoreboardName());
			participants.remove(player.getUUID());
		}
	}
	
	@SubscribeEvent
	public static void serverTick(LevelTickEvent event)
	{
		// This is a server-side tick only
		// Don't rat on players if setting is disabled
		if(event.side == LogicalSide.CLIENT || !Config.Server.EXPOSE_RATS.get())
			return;
		
		// Only rat on players at regular intervals
		if(System.currentTimeMillis() - lastSystemRat >= Config.Server.RAT_DELAY.get() * 1000) // Convert to ms
		{
			lastSystemRat = System.currentTimeMillis();
			Iterator<Entry<UUID, Participant>> iterator = participants.entrySet().iterator();
			
			while(iterator.hasNext())
			{
				Entry<UUID, Participant> entry = iterator.next();
				
				// First, check if participant is still in the server
				if(event.level.getServer().getPlayerList().getPlayer(entry.getKey()) == null)
				{
					// This should never happen btw
					CensorCraft.LOGGER.info("{} is not in the server anymore", entry.getValue().getName());
					iterator.remove();
					continue;
				}
				
				// If it's been longer than the allowed heartbeat
				if(System.currentTimeMillis() - entry.getValue().getLastHeartbeat() >= CensorCraft.HEARTBEAT_TIME)
				{
					event.level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(entry.getValue().getName()).withStyle(style -> style.withBold(true)).append(Component.literal(" doesn't have their mic on").withStyle(style -> style.withBold(false))), false);
				}
			}
		}
	}
	
	@SubscribeEvent
	public static void chatEvent(ServerChatEvent event)
	{
		if(Config.Server.MONITOR_CHAT.get())
		{
			new WordPacket(event.getRawText()).consume(event.getPlayer());
		}
	}
	
	private static void punish(ServerPlayer player)
	{
		Vec3 pos = player.position();
		
		if(Config.Server.ENABLE_EXPLOSION.get())
		{
			player.level().explode(null, player.level().damageSources().generic(), new ExplosionDamageCalculator(), pos.x, pos.y, pos.z, Config.Server.EXPLOSION_RADIUS.get(), Config.Server.EXPLOSION_FIRE.get(), Config.Server.EXPLOSION_GRIEFING.get() ? ExplosionInteraction.BLOCK : ExplosionInteraction.NONE);
		}
		
		if(Config.Server.ENABLE_LIGHTNING.get())
		{
			// Number of strikes
			for(int i = 0; i < Config.Server.LIGHTNING_STRIKES.get(); i++)
			{
				LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, player.level());
				bolt.setPos(pos);
				player.level().addFreshEntity(bolt);
			}
		}
		
		// borken
		if(Config.Server.ENABLE_SKY.get())
		{
			if(Config.Server.TELEPORT.get())
			{
				player.teleportTo(pos.x, pos.y + Config.Server.LAUNCH_HEIGHT.get(), pos.z);
			}
			else
			{
				player.addDeltaMovement(new Vec3(0, Config.Server.LAUNCH_HEIGHT.get(), 0));
				player.hurtMarked = true;
			}
		}
		
		if(Config.Server.ENABLE_IGNITE.get())
		{
			player.igniteForSeconds(Config.Server.IGNITE_SECONDS.get());
		}
		
		if(Config.Server.ENABLE_DIMENSION.get())
		{
			Config.Server.Dimension desiredDimension = Config.Server.DIMENSION.get();
			ResourceKey<Level> playerDimension = player.level().dimension();
			
			// If we are already in the desired dimension
			if(desiredDimension.toLevel() == playerDimension)
			{
				if(Config.Server.ENABLE_FALLBACK.get())
				{
					var fallback = Config.Server.FALLBACK_DIMENSION.get();
					
					// AND the fallback dimension is different
					// random.toLevel() is null so this should still work, assuming playerDimension will never be null
					if(fallback.toLevel() != playerDimension)
					{
						// Teleport player to fallback dimension
						tpDimension(fallback, player);
					}
					else
					{
						CensorCraft.LOGGER.warn("Failed to teleport {} to desired dimension. Player is in {}, and fallback dimension is {}", player.getScoreboardName(), playerDimension, fallback);
					}
				}
			}
			else
			{
				tpDimension(desiredDimension, player);
			}
		}
		
		if(Config.Server.ENABLE_POTION_EFFECTS.get())
		{
			for(String potion : Config.Server.POTION_EFFECTS.get())
			{
				// ig some potions have multiple effects (im a fake minecraft player)
				ForgeRegistries.POTIONS.getValue(ResourceLocation.withDefaultNamespace(potion)).getEffects().forEach(player::addEffect);
			}
		}
		
		if(Config.Server.ENABLE_ENTITIES.get())
		{
			Config.Server.ENTITIES.get().forEach(element -> ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.withDefaultNamespace(element)).spawn(player.serverLevel(), player.blockPosition(), EntitySpawnReason.COMMAND));
		}
		
		// If the player needs to die
		if(Config.Server.KILL_PLAYER.get())
		{
			// If we should ignore totems
			if(Config.Server.IGNORE_TOTEMS.get())
			{
				// Generic kill ignores totems
				player.kill(player.serverLevel());
			}
			else
			{
				// Generic will stop at totems
				player.hurtServer(player.serverLevel(), player.level().damageSources().generic(), Float.MAX_VALUE);
			}
		}
		
		if(Config.Server.ENABLE_COMMANDS.get())
		{
			MinecraftServer server = player.getServer();
			
			for(String command : Config.Server.COMMANDS.get())
			{
				// Ripped from (Base)CommandBlock
				try
				{
					server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
				} catch(Throwable throwable)
				{
					CrashReport crashreport = CrashReport.forThrowable(throwable, "Executing CensorCraft command");
					CrashReportCategory crashreportcategory = crashreport.addCategory("Command to be executed");
					crashreportcategory.setDetail("Command", command);
					throw new ReportedException(crashreport);
				}
			}
		}
	}
	
	// needs work
	private static void tpDimension(Config.Server.Dimension dimension, ServerPlayer player)
	{
		ResourceKey<Level> destDimension = dimension.toLevel();
		
		if(dimension == Config.Server.Dimension.RANDOM)
		{
			ResourceKey<Level> playerDimension = player.level().dimension();
			
			// Pick a dimension they aren't already in
			ResourceKey<Level> newDimension = playerDimension;
			
			while(newDimension == playerDimension)
			{
				// Won't pick random again because random's ordinal is 3
				newDimension = Config.Server.Dimension.values()[(int) (Math.random() * 3)].toLevel();
			}
			
			destDimension = newDimension;
		}
		
		// Ripped and grossly altered from NetherPortalBlock
		// Puts you within the bounds of the target dimension
		ServerLevel destLevel = player.getServer().getLevel(destDimension);
		double scale = DimensionType.getTeleportationScale(player.serverLevel().dimensionType(), destLevel.dimensionType());
		WorldBorder border = destLevel.getWorldBorder();
		Vec3 exitPos = border.clampToBounds(player.getX() * scale, player.getY(), player.getZ() * scale).getBottomCenter();
		
		// Find a place where you can put a portal as a fallback
		Vec3 colFree = PortalShape.findCollisionFreePosition(exitPos, destLevel, player, Player.STANDING_DIMENSIONS);
		BlockPos safePos = BlockPos.containing(colFree);
		
		// If nothing happened
		if(Config.Server.ENABLE_SAFE_TELEPORT.get())// && colFree.equals(exitPos))
		{
			// Heightmaps work fine elsewhere
			Optional<Entry<Heightmap.Types, Heightmap>> hi = destLevel.getChunkAt(safePos).getHeightmaps().stream().filter(entry -> entry.getKey() == Types.MOTION_BLOCKING).findFirst();
			
			if(hi.isPresent())
			{
				BlockPos candidatePos = new BlockPos(safePos.getX(), hi.get().getValue().getHighestTaken(safePos.getX() & 15, safePos.getZ() & 15), safePos.getZ());
				
				/**
				 * Attempts to put the player below the nether roof (safely!) if there's a free space to stand on
				 * 
				 * FLAWS: Block with grass with air blocks above don't work (I don't care)
				 */
				if(destDimension == Level.NETHER)
				{
					if(Config.Server.AVOID_NETHER_ROOF.get())
					{
						for(BlockPos y = candidatePos.below(); y.getY() > destLevel.dimensionType().minY(); y = y.below())
						{
							if(destLevel.getBlockState(y).entityCanStandOn(destLevel, y, player) && destLevel.getBlockStates(Player.STANDING_DIMENSIONS.makeBoundingBox(y.above().getBottomCenter())).allMatch(BlockState::isAir))
							{
								safePos = y;
								break;
							}
						}
					}
				}
				// If the heightmap gave us the void
				else if(candidatePos.getY() + 1 != destLevel.dimensionType().minY())
				{
					// There wasn't a place to put it (void) so just use the OG
					safePos = candidatePos;
				}
			}
			else
			{
				CensorCraft.LOGGER.warn("Failed to find a MOTION_BLOCkING heightmap position at {} in dimension {}", safePos, destDimension);
			}
			
			// Check if we're about to fall
			if(!destLevel.getBlockState(safePos).entityCanStandOn(destLevel, safePos, player) && Config.Server.SUMMON_DIRT.get())
			{
				System.out.println("SETTN DIRT");
				// Summon a dirt block to help
				destLevel.setBlockAndUpdate(safePos, Blocks.DIRT.defaultBlockState());
			}
		}
		
		player.teleport(new TeleportTransition(destLevel, safePos.above().getBottomCenter(), Vec3.ZERO, 0, 0, TeleportTransition.DO_NOTHING));
	}
}
