package io.github.freshsupasulley.censorcraft.config.punishments;

import java.util.List;
import java.util.function.Supplier;

import com.electronwill.nightconfig.core.serde.annotations.SerdeDefault;
import com.electronwill.nightconfig.core.serde.annotations.SerdeDefault.WhenValue;

import net.minecraft.server.level.ServerPlayer;

public class Commands extends PunishmentOption {
	
	@SerdeDefault(provider = "getDefault", whenValue = WhenValue.IS_NULL)
	private List<String> commands;
	public static transient Supplier<List<String>> getDefault = () -> List.of("clash of clans");
	
	@Override
	public void punish(ServerPlayer player)
	{
		// MinecraftServer server = player.getServer();
		
		for(String command : commands)
		{
			System.out.println("hey there! " + commands);
			// Ripped from (Base)CommandBlock
			// try
			// {
			// server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
			// } catch(Throwable throwable)
			// {
			// CrashReport crashreport = CrashReport.forThrowable(throwable, "Executing CensorCraft command");
			// CrashReportCategory crashreportcategory = crashreport.addCategory("Command to be executed");
			// crashreportcategory.setDetail("Command", command);
			// throw new ReportedException(crashreport);
			// }
		}
	}
	
}