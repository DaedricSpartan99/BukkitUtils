package atlas.commands;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.AbstractMap.SimpleEntry;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import atlas.messages.Message;

public class CommandManager implements CommandExecutor, TabCompleter {
	
	private Map<String, Entry<CommandGroup, Map<String, CommandWrapper>>> cmdMap = 
			new HashMap<String, Entry<CommandGroup, Map<String, CommandWrapper>>>();
	
	private String permissionDenied = "\u00a7cYou don't have permissions to perform this command",
					notAPlayer = "\u00a7cOnly a player can perform this command",
					notFound = "\u00a7Command not found";
	
	public void setPermissionDeniedMessage(String msg) {
		
		this.permissionDenied = msg;
	}
	
	public void setOnlyAPlayerMessage(String msg) {
		
		this.notAPlayer = msg;
	}
	
	public void setNotFoundMessage(String msg) {
		
		this.notFound = msg;
	}
	
	public void registerGroup(CommandGroup group, JavaPlugin plugin) throws IllegalArgumentException {
		
		Map<String, CommandWrapper> groupMap = new HashMap<String, CommandWrapper>();
		cmdMap.put(group.getCommand(), new SimpleEntry<CommandGroup, Map<String, CommandWrapper>>(group, groupMap));
		
		for (Method method : group.getClass().getMethods()) {
		
			CommandCallback ann = method.getAnnotation(CommandCallback.class);
			
			if (ann == null)
				continue;
			
			if (!method.getReturnType().getName().equals("boolean"))
				throw new IllegalArgumentException("A function " +
						" signed by CommandCallback must " +
						" have two parameters: (CommandSender sender, String[] args)" +
						" and should have boolean as return type");
			
			Class<?>[] params = method.getParameterTypes();
			CommandAccessibility access;
			
			if (params.length != 2) {
				throw new IllegalArgumentException("A function " +
						" signed by CommandCallback must " +
						" have two parameters: (CommandSender sender, String[] args)" +
						" and should have boolean as return type");
			} else {
				
				String sender = params[0].getName();
				
				if (sender.equals("org.bukkit.command.CommandSender"))
					access = CommandAccessibility.CONSOLE;
				else if (sender.equals("org.bukkit.entity.Player"))
					access = CommandAccessibility.PLAYER;
				else
					throw new IllegalArgumentException("A function " +
							" signed by CommandCallback must " +
							" have two parameters: (CommandSender sender, String[] args)" +
							" and should have boolean as return type");
				
				String args = params[0].getName();
				
				if (!args.equals("String[]"))
					throw new IllegalArgumentException("A function " +
							" signed by CommandCallback must " +
							" have two parameters: (CommandSender sender, String[] args)" +
							" and should have boolean as return type");
			}
			
			groupMap.put(ann.command(), new CommandWrapper(ann, access, method));
		}
		
		plugin.getCommand(group.getCommand()).setExecutor(this);
		plugin.getCommand(group.getCommand()).setTabCompleter(this);
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
		
		Entry<CommandGroup, Map<String, CommandWrapper>> groupEntry = cmdMap.get(cmd.getName());
		
		if (groupEntry == null)
			throw new NullPointerException("Command " + cmd.getName() + " not registered correctly");
		
		if (args.length == 0)
			return new ArrayList<String>(groupEntry.getValue().keySet());
		
		String subcommand = args[0];
		
		if (!groupEntry.getValue().containsKey(subcommand)) {
			
			boolean found = false;
			
			for (Entry<String, CommandWrapper> entry : groupEntry.getValue().entrySet()) {
				for (String al : entry.getValue().getCallback().aliases()) {
					if (args[0].equalsIgnoreCase(al)) {
						subcommand = entry.getKey();
						found = true;
						break;
					}
				}
			}
			
			if (!found)
				return null;
		}
		
		String[] subargs = Arrays.copyOfRange(args, 1, args.length);
		
		List<String> list = groupEntry.getKey().tabComplete(sender, subcommand, subargs);
		
		if (list == null) {
			
			String[] subs = groupEntry.getValue().get(subcommand).getCallback().tab();
			if (subs.length > 0)
				list = Arrays.asList(subs);
		}
		
		return list;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		
		Entry<CommandGroup, Map<String, CommandWrapper>> groupEntry = cmdMap.get(cmd.getName());
		
		if (groupEntry == null)
			throw new NullPointerException("Command " + cmd.getName() + " not registered correctly");
		
		if (args.length == 0)
			return false;
		
		String name = args[0];
		CommandWrapper wrapper = groupEntry.getValue().get(name);
		
		if (wrapper == null) {
			
			boolean found = false;
			
			for (Entry<String, CommandWrapper> entry : groupEntry.getValue().entrySet()) {
				
				for (String alias : entry.getValue().getCallback().aliases()) {
					if (args[0].equalsIgnoreCase(alias)) {
						name = entry.getKey();
						found = true;
						break;
					}
				}
				
				if (found)
					break;
			}
			
			if (!found) {
				Message.dynamicMessage(sender, notFound);
				return false;
			}
			
			wrapper = groupEntry.getValue().get(name);
		}
		
		String[] subargs = Arrays.copyOfRange(args, 1, args.length);
		
		for (String permission : wrapper.getCallback().permissions()) {
			if (!sender.hasPermission(permission)) {
				Message.dynamicMessage(sender, permissionDenied);
				return true;
			}
		}
		
		boolean call;
		
		switch (wrapper.getAccessibility()) {
		
		case CONSOLE:
			
			call = wrapper.invoke(groupEntry.getKey(), sender, subargs);
			break;
			
		case PLAYER:
			
			if (!(sender instanceof Player)) {
				Message.dynamicMessage(sender, notAPlayer);
				return true;
			}
			
			call = wrapper.invoke(groupEntry.getKey(), sender, subargs);
			break;
			
		default:
			call = true;
			break;
		}
		
		return call;
	}

	private class CommandWrapper {
		
		private CommandCallback callback;
		private CommandAccessibility access;
		private Method method;
		
		public CommandWrapper(CommandCallback callback, CommandAccessibility access, Method method) {
			
			this.callback = callback;
			this.access = access;
			this.method = method;
		}
		
		public CommandCallback getCallback() {
			
			return callback;
		}
		
		public CommandAccessibility getAccessibility() {
			
			return access;
		}
		
		public boolean invoke(CommandGroup group, CommandSender sender, String[] args) {
			
			boolean call = true;
			
			try {
				
				Object _call = method.invoke(group, sender, args);
				
				if (_call instanceof Boolean)
					call = (Boolean)_call;
				
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				Message.console("Cannot invoke command function");
				e.printStackTrace();
			}
			
			return call;
		}
	}
}
