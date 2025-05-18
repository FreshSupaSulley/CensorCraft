package com.supasulley.censorcraft.config.punishments;

import java.util.Map.Entry;
import java.util.Optional;

import com.supasulley.censorcraft.CensorCraft;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

public class Dimension extends PunishmentOption {
	
	private static ConfigValue<Boolean> ENABLE_SAFE_TELEPORT, AVOID_NETHER_ROOF, SUMMON_DIRT, ENABLE_FALLBACK;
	private static ConfigValue<VanillaDimensions> DIMENSION, FALLBACK_DIMENSION;
	
	@Override
	public String getDescription()
	{
		return "Sends the player to a dimension";
	}
	
	@Override
	public void build(Builder builder)
	{
		builder.comment("Using RANDOM means the player will be sent to a dimension they are not already in");
		DIMENSION = builder.comment("Dimension to send the player to").defineEnum("dimension", VanillaDimensions.NETHER);
		
		builder.comment("Tries to put the player in a safe position").push("safe_teleport");
		ENABLE_SAFE_TELEPORT = builder.define("enable", true);
		AVOID_NETHER_ROOF = builder.comment("Avoids putting the player on the nether roof (not a guarantee)").define("avoid_nether_roof", true);
		SUMMON_DIRT = builder.comment("Places a dirt block below the players feet if they're going to fall (useful in the end)").define("summon_dirt_block", true);
		ENABLE_FALLBACK = builder.comment("Sends the player to another dimension if they are already there").define("enable_fallback", true);
		FALLBACK_DIMENSION = builder.comment("Fallback dimension (enable_fallback must be true)").defineEnum("fallback", VanillaDimensions.RANDOM);
		builder.pop();
	}
	
	@Override
	public void punish(ServerPlayer player)
	{
		VanillaDimensions desiredDimension = DIMENSION.get();
		ResourceKey<Level> playerDimension = player.level().dimension();
		
		// If we are already in the desired dimension
		if(desiredDimension.toLevel() == playerDimension)
		{
			if(ENABLE_FALLBACK.get())
			{
				var fallback = FALLBACK_DIMENSION.get();
				
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
	
	// needs work
	private static void tpDimension(VanillaDimensions dimension, ServerPlayer player)
	{
		ResourceKey<Level> destDimension = dimension.toLevel();
		
		if(dimension == VanillaDimensions.RANDOM)
		{
			ResourceKey<Level> playerDimension = player.level().dimension();
			
			// Pick a dimension they aren't already in
			ResourceKey<Level> newDimension = playerDimension;
			
			while(newDimension == playerDimension)
			{
				// Won't pick random again because random's ordinal is 3
				newDimension = VanillaDimensions.values()[(int) (Math.random() * 3)].toLevel();
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
		if(ENABLE_SAFE_TELEPORT.get())// && colFree.equals(exitPos))
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
					if(AVOID_NETHER_ROOF.get())
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
			if(!destLevel.getBlockState(safePos).entityCanStandOn(destLevel, safePos, player) && SUMMON_DIRT.get())
			{
				System.out.println("SETTN DIRT");
				// Summon a dirt block to help
				destLevel.setBlockAndUpdate(safePos, Blocks.DIRT.defaultBlockState());
			}
		}
		
		player.teleport(new TeleportTransition(destLevel, safePos.above().getBottomCenter(), Vec3.ZERO, 0, 0, TeleportTransition.DO_NOTHING));
	}
	
	public static enum VanillaDimensions
	{
		
		OVERWORLD(Level.OVERWORLD), NETHER(Level.NETHER), END(Level.END), RANDOM(null);
		
		private ResourceKey<Level> level;
		
		VanillaDimensions(ResourceKey<Level> level)
		{
			this.level = level;
		}
		
		public ResourceKey<Level> toLevel()
		{
			return level;
		}
	}
}