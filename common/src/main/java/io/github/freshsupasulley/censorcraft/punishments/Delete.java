package io.github.freshsupasulley.censorcraft.punishments;

import io.github.freshsupasulley.censorcraft.api.punishments.PunishmentJob;
import io.github.freshsupasulley.censorcraft.api.punishments.PunishmentParam;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Delete extends BuiltInPunishment {
	
	// i reallllly hope this is ok to keep as a static var
	private static final Map<Block, String> BLOCK_MAP = BuiltInRegistries.BLOCK.stream().collect(Collectors.toMap(block -> block, block -> block.getName().getString().toLowerCase()));
	
	private static final List<String> BLOCK_NAMES = BLOCK_MAP.values().stream().toList();
	private static final List<String> NORMALIZED_BLOCK_NAMES = BLOCK_NAMES.stream().map(Delete::normalizeBlockName).map(String::toLowerCase).toList();
	
	private static String normalizeBlockName(String name)
	{
		name = name.toLowerCase(Locale.ROOT).trim();
		
		// Remove common prefixes/suffixes
		name = name.replaceFirst("^block of ", "");
		name = name.replaceFirst(" block$", "");
		name = name.replaceFirst(" ore$", "");
		
		return name;
	}
	
	@Override
	public String getId()
	{
		return "delete";
	}
	
	@Override
	public void buildConfig()
	{
		defineInRange("radius", 5, 1, Integer.MAX_VALUE); // it seems by not defining a range, forge thinks the config file is broken
		define("cubic", true, "Use a cubic area rather than a sphere");
		define("exact_match", true, "Require exact block names (i.e. you must say Grass Block instead of Grass)");
		define("block_name_contains", new ArrayList<>(List.of("coral", "door", "log")), "Delete blocks whose translated names contain any of these strings");
	}
	
	@Override
	public PunishmentJob punish(ServerPlayer player, Set<String> taboos)
	{
		final int radius = ((Number) config.get("radius")).intValue(); // this missing probably deserves an error thrown in console
		final boolean cubic = config.getOrElse("cubic", false); // this doesn't
		final boolean exactMatch = config.getOrElse("exact_match", true);
		final List<String> partialBlocks = config.getOrElse("block_name_contains", List.of(""));
		
		// Queue a bigass job to delete potentially millions of blocks near the player
		return new DeleteJob(player, radius, cubic, exactMatch, taboos, new LinkedHashSet<>(partialBlocks));
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <T> T getParam(PunishmentParam<T> param, T fallback)
	{
		// Hacky way to ignore the users taboos
		if(param == PunishmentParam.TABOOS)
		{
			final boolean exactMatch = config.getOrElse("exact_match", true);
			
			var result = exactMatch ? BLOCK_NAMES : NORMALIZED_BLOCK_NAMES;
			
			// Normalize based on block_name_contains
			var newList = config.getOrElse("block_name_contains", List.of(""));
			
			if(newList.isEmpty())
			{
				return (T) result;
			}
			
			// If result has "Grass Block" and newList has "Grass", remove "Grass Block" from result
			return (T) Stream.concat(result.stream().filter(name -> newList.stream().noneMatch(name::contains)), newList.stream()).toList();
		}
		
		return super.getParam(param, fallback);
	}
	
	static class DeleteJob extends PunishmentJobImpl {
		
		int radius;
		boolean cubic, exactMatch;
		Set<String> taboos, partialBlocks;
		
		private final int cx, cy, cz;
		
		// Progression tracking
		private int index;
		private final int side, total;
		
		public DeleteJob(ServerPlayer player, int radius, boolean cubic, boolean exactMatch, Set<String> taboos, Set<String> partialBlocks)
		{
			super(player.getUUID());
			
			this.radius = radius;
			this.cubic = cubic;
			this.exactMatch = exactMatch;
			this.taboos = taboos;
			this.partialBlocks = partialBlocks;
			
			var blockPos = player.blockPosition();
			cx = blockPos.getX();
			cy = blockPos.getY();
			cz = blockPos.getZ();
			
			// Init state
			this.side = radius * 2 + 1;
			this.total = side * side * side;
		}
		
		@Override
		public boolean tick(long endTimeMs, ServerPlayer player)
		{
			var level = player.level();
			var pos = new BlockPos.MutableBlockPos();
			int radiusSq = radius * radius;
			
			// worlds (maybe 2nd?) ever intentional do while loop (ts has to run at least once)
			do
			{
				// Exit if there's more to do
				if(index >= total)
					return true;
				
				int layerSize = side * side;
				
				int yIndex = index / layerSize;
				int rem = index % layerSize;
				
				int xIndex = rem / side;
				int zIndex = rem % side;
				
				int x = xIndex - radius;
				int y = radius - yIndex; // top to bottom
				int z = zIndex - radius;
				
				// IMPORTANT: coords are now derived from index. This is the earliest place we can increment it
				index++;
				
				// Skip positions outside the sphere if requested
				if(cubic || x * x + y * y + z * z <= radiusSq)
				{
					pos.set(cx + x, cy + y, cz + z);
					
					BlockState state = level.getBlockState(pos);
					
					if(state.isAir())
						continue;
					
					// lil faster than state.getBlock().getName().getString().toLowerCase()
					String blockName = BLOCK_MAP.get(state.getBlock());
					
					if(taboos.stream().anyMatch(blockName::contains))
					{
						level.destroyBlock(pos, false);
					}
				}
			}
			while(System.currentTimeMillis() < endTimeMs);
			
			// Incomplete
			return false;
		}
	}
}
