package io.github.freshsupasulley.censorcraft.config.punishments;

import java.util.Map.Entry;
import java.util.Optional;

import io.github.freshsupasulley.censorcraft.CensorCraft;
import io.github.freshsupasulley.censorcraft.api.punishments.Punishment;
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

public class Dimension extends Punishment<Dimension> {
	
	@Override
	public void build()
	{
		// builder.comment("Using RANDOM means the player will be sent to a dimension they are not already in");
		defineEnum("dimension", VanillaDimensions.NETHER, "Dimension to send the player to");
		define("safe_teleport", true, "Tries to put the player in a safe position");
		define("avoid_nether_roof", true, "Avoids putting the player on the nether roof (not a guarantee)");
		define("summon_dirt_block", true, "Places a dirt block below the players feet if they're going to fall (useful in the end)");
		define("enable_fallback", true, "Sends the player to another dimension if they are already there");
		defineEnum("fallback", VanillaDimensions.RANDOM, "Fallback dimension (enable_fallback must be true)");
	}
	
	@Override
	public void punish(ServerPlayer player)
	{
		VanillaDimensions desiredDimension = config.getEnum("dimension", VanillaDimensions.class);
		ResourceKey<Level> playerDimension = player.level().dimension();
		
		// If we are already in the desired dimension
		if(desiredDimension.toLevel() == playerDimension)
		{
			boolean enableFallback = config.get("enable_fallback");
			
			if(enableFallback)
			{
				VanillaDimensions fallback = config.getEnum("fallback", VanillaDimensions.class);
				
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
	private void tpDimension(VanillaDimensions dimension, ServerPlayer player)
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
		double scale = DimensionType.getTeleportationScale(player.level().dimensionType(), destLevel.dimensionType());
		WorldBorder border = destLevel.getWorldBorder();
		Vec3 exitPos = border.clampToBounds(player.getX() * scale, player.getY(), player.getZ() * scale).getBottomCenter();
		
		// Find a place where you can put a portal as a fallback
		Vec3 colFree = PortalShape.findCollisionFreePosition(exitPos, destLevel, player, Player.STANDING_DIMENSIONS);
		BlockPos safePos = BlockPos.containing(colFree);
		
		// If nothing happened
		boolean safeTeleport = config.get("safe_teleport");
		
		if(safeTeleport)// && colFree.equals(exitPos))
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
					boolean avoidNetherRoof = config.get("avoid_nether_roof");
					
					if(avoidNetherRoof)
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
			boolean summonDirt = config.get("summon_dirt_block");
			
			if(!destLevel.getBlockState(safePos).entityCanStandOn(destLevel, safePos, player) && summonDirt)
			{
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
	
	@Override
	public Dimension newInstance()
	{
		return new Dimension();
	}
}