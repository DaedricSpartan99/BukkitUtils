package atlas.messages;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import net.minecraft.server.v1_8_R3.IChatBaseComponent.ChatSerializer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import atlas.config.Config;

public class Messages {
	
	private static String PREFIX = "";
	private static Map<Message, MessagePack> messages = new HashMap<Message, MessagePack>();
	
	public static MessagePack getMessage(Message entry) {
		
		return messages.get(entry);
	}
	
	public static String getPrefix() {
		
		return PREFIX;
	}
	
	public static void setPrefix(String prefix) {
		
		if (prefix == null)
			PREFIX = "";
		else
			PREFIX = prefix;
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends Enum<?> & Message> void loadMessages(Config config, Class<T>... groups) {
		
		boolean mod = false;
		
		if (!messages.isEmpty())
			messages.clear();
		
		if (config.getConfig().isString("prefix"))
			PREFIX = Config.staticReadFormat(config.getConfig().getString("prefix"));
		else {
			config.getConfig().createSection("prefix");
			config.getConfig().set("prefix", Config.staticWriteFormat(PREFIX));
			mod = true;
		}
		
		ConfigurationSection section = config.getConfig().getConfigurationSection("groups");
			
		if (section == null)
			section = config.getConfig().createSection("groups");
		
		for (Class<T> group : groups) {
			
			String name;
			
			if (group.isAnnotationPresent(MessageGroup.class))
				name = group.getAnnotation(MessageGroup.class).name();
			else
				name = group.getName().replaceAll("\\.", "_");
			
			ConfigurationSection groupSection = section.getConfigurationSection(name);
			
			if (groupSection == null)
				groupSection = section.createSection(name);
			
			Message[] messages = group.getEnumConstants();
			
			for (Message message : messages) {
				
				String pack;
				
				if (groupSection.isString(message.getName()))
					pack = groupSection.getString(message.getName());
				else {
					groupSection.createSection(message.getName());
					groupSection.set(message.getName(), pack = "");
					mod = true;
				}
				
				pack = Config.staticReadFormat(pack);
					
				Messages.messages.put(message, new MessagePack(message, pack));
			}
		}
		
		if (mod)
			config.save();
	}
	
	public static void dynamicMessage(CommandSender sender, String msg) {
		
		sender.sendMessage(PREFIX + msg);
	}
	
	public static void console(String msg) {
		
		Bukkit.getConsoleSender().sendMessage(PREFIX + msg);
	}
	
	public static String formatArgs(String msg, Object... args) {
		
		String out = msg;
		
		for (Object obj : args) {
			
			if (obj instanceof Integer)
				out = Messages.formatInt(out, (Integer)obj);
			else if (obj instanceof Long)
				out = Messages.formatLong(out, (Long)obj);
			else if (obj instanceof Short)
				out = Messages.formatInt(out, (Short)obj);
			else if (obj instanceof Byte)
				out = Messages.formatInt(out, (Byte)obj);
			else if (obj instanceof Double)
				out = Messages.formatDouble(out, (Double)obj);
			else if (obj instanceof Float)
				out = Messages.formatFloat(out, (Float)obj);
			else if (obj instanceof OfflinePlayer)
				out = Messages.formatOfflinePlayer(out, (OfflinePlayer)obj);
			else if (obj instanceof Player)
				out = Messages.formatPlayer(out, (Player)obj);
			else if (obj instanceof String)
				out = Messages.formatString(out, (String)obj);
			else if (obj instanceof ChatColor)
				out = Messages.formatColor(out, (ChatColor)obj);
		}
		
		return out;
	}
	
	public static void sendActionBar(Player player, String msg) {
		
        IChatBaseComponent icbc = ChatSerializer.a("{\"text\": \"" + msg + "\"}");
        PacketPlayOutChat bar = new PacketPlayOutChat(icbc, (byte)2);
        ((CraftPlayer)player).getHandle().playerConnection.sendPacket(bar);
	}
	
	public static String formatPlayer(String input, Player p) {
		
		return input.replaceFirst("%player%", p.getDisplayName());
	}
	
	public static String formatOfflinePlayer(String input, OfflinePlayer p) {
		
		return input.replaceFirst("%player%", p.getName());
	}
	
	public static String formatInt(String input, int... ns) {
		
		// %d e' una convenzione del linguaggio C, dai un occhiata a sprintf()
		
		String out = input;
		
		for (int n : ns)
			out = out.replaceFirst("%d", String.valueOf(n));
		
		return out;
	}
	
	public static String formatLong(String input, long... ns) {
		
		// %d e' una convenzione del linguaggio C, dai un occhiata a sprintf()
		
		String out = input;
		
		for (long n : ns)
			out = out.replaceFirst("%d", String.valueOf(n));
		
		return out;
	}
	
	public static String formatDouble(String input, double... ns) {
		
		// %f e' una convenzione del linguaggio C, dai un occhiata a sprintf()
		
		String out = input;
		
		for (double n : ns)
			out = out.replaceFirst("%f", String.valueOf(n));
		
		return out;
	}
	
	public static String formatFloat(String input, float... ns) {
		
		// %f e' una convenzione del linguaggio C, dai un occhiata a sprintf()
		
		String out = input;
		
		for (float n : ns)
			out = out.replaceFirst("%f", String.valueOf(n));
		
		return out;
	}
	
	public static String formatString(String input, String... ns) {
		
		// %s e' una convenzione del linguaggio C, dai un occhiata a sprintf()
		
		String out = input;
		
		for (String n : ns)
			out = out.replaceFirst("%s", n);
		
		return out;
	}
	
	public static String formatColor(String input, ChatColor...colors) {
		
		String out = input;
		
		for (ChatColor n : colors)
			out = out.replaceFirst("%c", String.valueOf(n));
		
		return out;
	}
}
