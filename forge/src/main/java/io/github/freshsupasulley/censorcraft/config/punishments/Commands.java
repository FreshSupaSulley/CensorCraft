package io.github.freshsupasulley.censorcraft.config.punishments;

import java.util.List;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

public class Commands extends PunishmentOption {
	
	private static ConfigValue<List<? extends String>> COMMANDS;
	
	@Override
	public void build(Builder builder)
	{
		COMMANDS = builder.defineListAllowEmpty("commands", List.of("/kill @a"), element -> true);
	}
	
	@Override
	public void punish(ServerPlayer player)
	{
		MinecraftServer server = player.getServer();
		
		for(String command : COMMANDS.get())
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