package io.github.freshsupasulley.censorcraft.config.punishments;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class Commands extends ForgePunishment {
	
	@SuppressWarnings("unchecked")
	@Override
	public void punish(ServerPlayer player)
	{
		MinecraftServer server = player.getServer();
		
		for(String command : (List<String>) config.get("commands"))
		{
			// Ripped from (Base)CommandBlock
			try
			{
				// Create the commandstack from the player, but override their permissions with the op permission level
				// Without that, unopped players that trigger this punishment won't be able to run this command
				var stack = player.createCommandSourceStack().withPermission(server.getOperatorUserPermissionLevel());
				
				// Suppress the output to not clog chat
				if(config.get("suppress_output"))
				{
					stack = stack.withSuppressedOutput();
				}
				
				server.getCommands().performPrefixedCommand(stack, command);
			} catch(Throwable throwable)
			{
				CrashReport crashreport = CrashReport.forThrowable(throwable, "Failed to execute CensorCraft command");
				CrashReportCategory crashreportcategory = crashreport.addCategory("Command to be executed");
				crashreportcategory.setDetail("Command", command);
				throw new ReportedException(crashreport);
			}
		}
	}
	
	@Override
	public void buildConfig()
	{
		define("commands", new ArrayList<>(List.of("/setblock ~ ~ ~ minecraft:lava")), "List of commands to be ran in order", "Commands are ran as the player with max permissions");
		define("suppress_output", true, "Don't send the command to chat");
	}
}