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
	
	@Override
	public void buildConfig()
	{
		define("commands", new ArrayList<>(List.of("")));
	}
}