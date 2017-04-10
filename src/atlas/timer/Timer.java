package atlas.timer;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public abstract class Timer implements Runnable {
	
	private int seconds;
	private final int total;
	private final int id;
	private volatile boolean sigint = false;
	
	public static final int BUKKIT_SECOND = 20;
	
	public Timer(Plugin plugin, int seconds) {
		
		this.seconds = seconds;
		this.total = seconds;
		
		if(this.seconds < 0)
			this.seconds = -seconds;
			
		id = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this, 0, BUKKIT_SECOND);
	}
	
	public void interrupt() {
		
		sigint = true;
	}
	
	public void forceInterrupt() {
		
		Bukkit.getServer().getScheduler().cancelTask(id);
	}
	
	public void restart() {
		
		this.seconds = total;
	}

	public void run() {
		
		handleSecond(seconds);
			
		if (sigint) {
			
			Bukkit.getServer().getScheduler().cancelTask(id);
			sigint = false;
			return;
		}
		
		seconds--;
		
		if (seconds == -1) {
			
			endAction();
			Bukkit.getServer().getScheduler().cancelTask(id);
		}
	}
	
	public abstract void handleSecond(int second);
	
	public abstract void endAction();
}

