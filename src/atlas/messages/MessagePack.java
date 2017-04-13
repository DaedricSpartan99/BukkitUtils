package atlas.messages;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import org.bukkit.entity.Player;

public class MessagePack {
	
	private final Message message;
	private final String pack;
	
	MessagePack (Message message, String pack) {
		
		this.message = message;
		this.pack = pack;
	}
	
	public Message getMessage() {
		
		return message;
	}
	
	public String get(Object... args) {
		
		return Messages.formatArgs(pack, args);
	}
	
	@SuppressWarnings("deprecation")
	public void send(CommandSender sender, Object... args) {
		
		String msg = Messages.formatArgs(pack, args);
		
		switch (message.getType()) {
			
		case MESSAGE:
			sender.sendMessage(Messages.getPrefix() + msg);
			break;
		case ACTIONBAR:
			if (sender instanceof Player)
				Messages.sendActionBar((Player)sender, msg);
			break;
		case TITLE:
			if (sender instanceof Player)
				((Player)sender).sendTitle(msg, null);
			break;
		default:
			sender.sendMessage(msg);
			break;
		}
	}
	
	@SuppressWarnings("deprecation")
	public void broadcast(Object... args) {
		
		String msg = Messages.formatArgs(pack, args);
		
		switch (message.getType()) {
			
		case MESSAGE:
			Bukkit.broadcastMessage(Messages.getPrefix() + msg);
			break;
		case ACTIONBAR:
			for (Player player : Bukkit.getOnlinePlayers())
				Messages.sendActionBar(player, msg);
			break;
		case TITLE:
			for (Player player : Bukkit.getOnlinePlayers())
				player.sendTitle(msg, null);
		default:
			Bukkit.broadcastMessage(msg);
			break;
		}
	}
}
