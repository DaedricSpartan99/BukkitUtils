package atlas.communication.server;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import atlas.communication.callback.ComCallback;


public class ClientList implements Runnable {
	
	private volatile Map<String, ClientHandler> handlers = new HashMap<String, ClientHandler>();
	private List<ComCallback> listeners = new ArrayList<ComCallback>();
	private int taskID;
	
	@SuppressWarnings("deprecation")
	public ClientList(Plugin plugin, long checkTicks) {
		
		taskID = Bukkit.getScheduler().scheduleAsyncRepeatingTask(plugin, this, 1L, checkTicks);
	}
	
	public synchronized void addHandler(ClientHandler handler) {
		
		handlers.put(handler.getHostname(), handler);
	}
	
	public ClientHandler getHandler(String ip) {
		
		return handlers.get(ip);
	}
	
	public synchronized void addClientCallback(ComCallback cb) {
		
		if (!listeners.contains(cb))
			listeners.add(cb);
	}
	
	public synchronized void removeClientCallback(ComCallback cb) {
		
		if (listeners.contains(cb))
			listeners.remove(cb);
	}
	
	public synchronized void stop() {
		
		Bukkit.getScheduler().cancelTask(taskID);
	}
	
	@Override
	public void run() {
		
		for (Entry<String, ClientHandler> entry : handlers.entrySet())
			entry.getValue().run();
	}

	public class ClientHandler {
		
		private Socket socket;
		private String ip;
		private int offset;
		private DataInputStream inputStream;
		private DataOutputStream outputStream;
		
		public ClientHandler(String ip, Socket socket) throws IOException {
			
			this.socket = socket;
			this.ip = ip;
			
			this.offset = 0;
			this.inputStream = new DataInputStream(socket.getInputStream());
			this.outputStream = new DataOutputStream(socket.getOutputStream());
		}
		
		public String getHostname() {
			
			return ip;
		}
		
		public Socket getSocket() {
			
			return socket;
		}
		
		public synchronized void write(byte[] data) {
			
			if (data.length == 0)
				return;
			
			try {
				
				outputStream.writeInt(data.length);	// size is first data
				outputStream.write(data, this.offset, data.length);	// raw data
				outputStream.flush();
				
				this.offset += data.length + 4;	// integer + byte array
					
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		
		public void close() {
			
			try {
				
				this.inputStream.close();
				this.outputStream.close();
				this.socket.close();
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void run() {
			
			byte[] data;
			
			try {
				
				if (inputStream.available() == 0)
					return;		// this skips until data are written down
			
				data = new byte[inputStream.readInt()];
					
				if (data.length == 0)
					return;
				
				this.offset += 4;	// integer size
					
				int written = inputStream.read(data, this.offset, data.length);
				
				if (written < 0) {
					System.err.print("Corrupted client data");
					return;
				}
				
				this.offset += data.length;	// byte array size
				
			} catch (IOException ex) {
				
				System.err.print("Corrupted client data");
				ex.printStackTrace();
				return;
			}
				
			ByteArrayOutputStream outData = new ByteArrayOutputStream();
			
			for (ComCallback listener : listeners) {
				
				try {
					
					listener.communicate(ip, data, outData);
					
				} catch (Exception ex) {
					new ServerCommunicationException(ip, ex).printStackTrace();
				}
			}
				
			this.write(outData.toByteArray());
		}
	}
	
	public class ServerCommunicationException extends Exception {

		private static final long serialVersionUID = 1L;
		
		public ServerCommunicationException(String ip, Throwable cause) {
			
			super("Unhandled exception while communicating with the client " + ip, cause, false, true);
		}
	}
}
