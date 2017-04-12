package atlas.commands;

import java.util.List;

import org.bukkit.command.CommandSender;

public interface CommandGroup {

	String getCommand();
	List<String> tabComplete(CommandSender sender, String subcommand, String[] args);
}
