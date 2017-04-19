package atlas.communication.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import atlas.communication.callback.ComCallback;
import atlas.communication.server.ClientList.ClientHandler;

public class ServerChannel implements Runnable {
	
	private ServerSocket socket;
	private ClientList clientList;
	private volatile boolean stop;
	private int taskID;
	
	public static final int DEFAULT_SERVER_PORT = 9990,
							DEFAULT_BACKLOG = 20;
	public static final long DEFAULT_CHECK_TICKS = 20L;
	
	public ServerChannel(Plugin plugin) {
		
		this(Bukkit.getServer().getIp(), DEFAULT_SERVER_PORT, DEFAULT_BACKLOG, DEFAULT_CHECK_TICKS, plugin);
	}
	
	public ServerChannel(String hostname, Plugin plugin) {
		
		this(hostname, DEFAULT_SERVER_PORT, DEFAULT_BACKLOG, DEFAULT_CHECK_TICKS, plugin);
	}
	
	public ServerChannel(int port, Plugin plugin) {
		
		this(Bukkit.getServer().getIp(), port, DEFAULT_BACKLOG, DEFAULT_CHECK_TICKS, plugin);
	}
	
	public ServerChannel(String hostname, int port, Plugin plugin) {
		
		this(hostname, port, DEFAULT_BACKLOG, DEFAULT_CHECK_TICKS, plugin);
	}
	
	@SuppressWarnings("deprecation")
	public ServerChannel(String hostname, int port, int backlog, long checkTicks, Plugin plugin) {
		
		try {
			
			this.socket = new ServerSocket();
			this.socket.bind(new InetSocketAddress(hostname, backlog), port);
			
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		clientList = new ClientList(plugin, checkTicks);
				
		stop = false;
		taskID = Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, this, 1L);
	}
	
	public String getIp() {
		
		return this.socket.getInetAddress().getHostName();
	}
	
	public ClientHandler getHandler(String ip) {
		
		return clientList.getHandler(ip);
	}
	
	public synchronized void addClientCallback(ComCallback cb) {
		
		clientList.addClientCallback(cb);
	}
	
	public synchronized void removeClientCallback(ComCallback cb) {
		
		clientList.addClientCallback(cb);
	}
	
	public synchronized void stop() {
		
		stop = true;
		Bukkit.getScheduler().cancelTask(taskID);
		clientList.stop();
	}
	
	@Override
	public void run() {
		
		while (!stop) {
			
			try {
				
				Socket client = this.socket.accept();
				client.setKeepAlive(true);
				client.setTcpNoDelay(true);
				String ip = this.getIp();
				
				this.clientList.addHandler(clientList.new ClientHandler(ip, client));
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
