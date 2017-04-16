package atlas.commands;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import atlas.messages.Messages;

public class CommandManager implements CommandExecutor, TabCompleter {
	
	private Map<String, GroupWrapper> cmdMap = 
			new HashMap<String, GroupWrapper>();
	
	private String permissionDenied = "\u00a7cYou don't have permissions to perform this command",
					notAPlayer = "\u00a7cOnly a player can perform this command",
					notFound = "\u00a7cCommand not found";
	
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
		
		GroupWrapper gWrap = new GroupWrapper(group);
		
		for (Method method : group.getClass().getDeclaredMethods()) {
		
			MainCommand mainAnn = method.getAnnotation(MainCommand.class);
			CommandCallback ann = method.getAnnotation(CommandCallback.class);
			
			if (ann == null && mainAnn == null)
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
				
				String args = params[1].getName();
				
				if (!args.equals(String[].class.getName()))
					throw new IllegalArgumentException("A function " +
							" signed by CommandCallback must " +
							" have two parameters: (CommandSender sender, String[] args)" +
							" and should have boolean as return type");
			}
			
			if (mainAnn != null) {
				gWrap.setMainCommand(method, access);
				Messages.console("Command " + group.getCommand() + " registered correctly");
			} else if (ann != null) {
				gWrap.addSubcommand(ann.command(), new CommandWrapper(ann, access, method));
				Messages.console("Subcommand " + ann.command() + " registered correctly");
			}
		}
		
		cmdMap.put(group.getCommand(), gWrap);
		
		plugin.getCommand(group.getCommand()).setExecutor(this);
		plugin.getCommand(group.getCommand()).setTabCompleter(this);
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
		
		GroupWrapper gWrap = cmdMap.get(cmd.getName());
		
		if (gWrap == null)
			throw new NullPointerException("Command " + cmd.getName() + " not registered correctly");
		
		if (args.length == 0)
			return new ArrayList<String>(gWrap.getSubcommandNames());
		
		String subcommand = args[0];
		
		if (!gWrap.contains(subcommand)) {
			
			if (args.length > 1)
				return null;
			
			List<String> possibilities = new ArrayList<String>();
			
			for (Entry<String, CommandWrapper> entry : gWrap.getSubcommands()) {
				
				if (entry.getKey().startsWith(subcommand))
					possibilities.add(entry.getKey());
				
				for (String al : entry.getValue().getCallback().aliases()) {
					if (al.startsWith(subcommand))
						possibilities.add(al);
				}
			}
			
			return possibilities;
		}
		
		String[] subargs = Arrays.copyOfRange(args, 1, args.length);
		
		List<String> list = gWrap.getGroup().tabComplete(sender, subcommand, subargs);
		
		if (list == null) {
			
			String[] subs = gWrap.getSubcommand(subcommand).getCallback().tab();
			if (subs.length > 0)
				list = Arrays.asList(subs);
		}
		
		return list;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		
		GroupWrapper gWrap = cmdMap.get(cmd.getName());
		
		if (gWrap == null)
			throw new NullPointerException("Command " + cmd.getName() + " not registered correctly");
		
		if (args.length == 0) {
			
			boolean call = false;
			
			if (gWrap.hasMainCommand()) {
				
				call = true;
				
				if (!gWrap.hasMainAccessibility(sender))
					Messages.dynamicMessage(sender, notAPlayer);
				else if (!gWrap.hasMainPermissions(sender))
					Messages.dynamicMessage(sender, permissionDenied);
				else 
					call = gWrap.invokeMain(sender, args);
			}
			
			return call;
		}
		
		String name = args[0];
		CommandWrapper wrapper = gWrap.getSubcommand(name);
		
		if (wrapper == null) {
			
			boolean found = false;
			
			for (Entry<String, CommandWrapper> entry : gWrap.getSubcommands()) {
				
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
				
				boolean call = false;
				
				if (gWrap.hasMainCommand()) {
					
					call = true;
					
					if (!gWrap.hasMainAccessibility(sender))
						Messages.dynamicMessage(sender, notAPlayer);
					else if (!gWrap.hasMainPermissions(sender))
						Messages.dynamicMessage(sender, permissionDenied);
					else 
						call = gWrap.invokeMain(sender, args);
					
				} else
					Messages.dynamicMessage(sender, notFound);
				
				return call;
			}
			
			wrapper = gWrap.getSubcommand(name);
		}
		
		String[] subargs = Arrays.copyOfRange(args, 1, args.length);
		
		boolean call = true;
		
		if (!wrapper.hasPermissions(sender))
			Messages.dynamicMessage(sender, permissionDenied);
		else if (!wrapper.hasAccessibility(sender))
			Messages.dynamicMessage(sender, notAPlayer);
		else 
			call = wrapper.invoke(gWrap.getGroup(), sender, subargs);
		
		return call;
	}
	
	private class GroupWrapper {
		
		private CommandGroup group;
		private MainCommand mainCallback = null;
		private Method mainCommand = null;
		private CommandAccessibility mainAccessibility = null;
		private Map<String, CommandWrapper> subMap = new HashMap<String, CommandWrapper>();
		
		public GroupWrapper(CommandGroup group) {
			
			this.group = group;
		}
		
		public void setMainCommand(Method mainCommand, CommandAccessibility access) {
			
			this.mainCallback = mainCommand.getAnnotation(MainCommand.class);
			this.mainCommand = mainCommand;
			this.mainAccessibility = access;
		}
		
		public boolean hasMainCommand() {
			
			return mainCallback != null;
		}
		
		public boolean hasMainAccessibility(CommandSender sender) {
			
			switch (mainAccessibility) {
			
			case CONSOLE:
				break;
				
			case PLAYER:
				
				if (!(sender instanceof Player))
					return false;
			}
			
			return true;
		}
		
		public boolean hasMainPermissions(CommandSender sender) {
			
			for (String permission : mainCallback.permissions()) {
				if (sender.hasPermission(permission))
					return true;
			}
			
			return false;
		}
		
		public boolean invokeMain(CommandSender sender, String[] args) {
			
			boolean call = true;
			
			try {
				
				Object _call = mainCommand.invoke(group, sender, args);
				
				if (_call instanceof Boolean)
					call = (Boolean)_call;
				
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
			
			return call;
		}
		
		public void addSubcommand(String name, CommandWrapper wrapper) {
			
			subMap.put(name, wrapper);
		}
		
		public CommandWrapper getSubcommand(String name) {
			
			return subMap.get(name);
		}
		
		public Set<Entry<String, CommandWrapper>> getSubcommands() {
			
			return subMap.entrySet();
		}
		
		public Set<String> getSubcommandNames() {
			
			return subMap.keySet();
		}
		
		public boolean contains(String name) {
			
			return subMap.containsKey(name);
		}
		
		public CommandGroup getGroup() {
			
			return group;
		}
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
		
		public boolean hasPermissions(CommandSender sender) {
			
			for (String permission : callback.permissions()) {
				if (sender.hasPermission(permission))
					return true;
			}
			
			return false;
		}
		
		public boolean hasAccessibility(CommandSender sender) {
			
			switch (access) {
			
			case CONSOLE:
				break;
				
			case PLAYER:
				
				if (!(sender instanceof Player))
					return false;
			}
			
			return true;
		}
		
		public boolean invoke(CommandGroup group, CommandSender sender, String[] args) {
			
			boolean call = true;
			
			try {
				
				Object _call = method.invoke(group, sender, args);
				
				if (_call instanceof Boolean)
					call = (Boolean)_call;
				
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				Messages.console("Cannot invoke command function");
				e.printStackTrace();
			}
			
			return call;
		}
	}
}
