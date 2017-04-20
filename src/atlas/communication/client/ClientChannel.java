package atlas.communication.client;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import atlas.communication.callback.ComCallback;
import atlas.communication.server.ServerChannel;

public class ClientChannel implements Runnable {
	
	public static final int DEFAULT_LOCAL_PORT = 9990;

	private Socket socket;
	private String serverIP;
	private int offset;
	private DataInputStream inputStream;
	private DataOutputStream outputStream;
	private int taskID = -1;
	
	private List<ComCallback> listeners = new ArrayList<ComCallback>();
	
	public ClientChannel(String serverIP, Plugin plugin) throws UnknownHostException {
		
		this(InetAddress.getByName(serverIP), InetAddress.getLocalHost(), plugin);
	}
	
	public ClientChannel(String serverIP, String local, Plugin plugin) throws UnknownHostException {
		
		this(InetAddress.getByName(serverIP), InetAddress.getByName(local), plugin);
	}
	
	public ClientChannel(InetAddress serverIP, InetAddress local, Plugin plugin) {
		
		this(serverIP, local, ServerChannel.DEFAULT_SERVER_PORT, DEFAULT_LOCAL_PORT,
				20L, plugin);
	}
	
	public ClientChannel(String serverIP, String local, long checkTicks, Plugin plugin) throws UnknownHostException {
		
		this(InetAddress.getByName(serverIP), InetAddress.getByName(local), checkTicks, plugin);
	}
	
	public ClientChannel(InetAddress serverIP, InetAddress local, long checkTicks, Plugin plugin) {
		
		this(serverIP, local, ServerChannel.DEFAULT_SERVER_PORT, DEFAULT_LOCAL_PORT,
				checkTicks, plugin);
	}
	
	@SuppressWarnings("deprecation")
	public ClientChannel(InetAddress serverIP, InetAddress local, int serverport, 
			int localPort, long checkTicks, Plugin plugin) {
		
		try {
			
			this.socket = new Socket(serverIP, serverport, local, localPort);
			
			this.serverIP = serverIP.getHostName();
			this.offset = 0;
			this.inputStream = new DataInputStream(socket.getInputStream());
			this.outputStream = new DataOutputStream(socket.getOutputStream());
			
		} catch (UnknownHostException ex) {
		
			ex.printStackTrace();
			
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		taskID = Bukkit.getScheduler().scheduleAsyncRepeatingTask(plugin, this, 1L, checkTicks);
	}
	
	public synchronized void addClientCallback(ComCallback cb) {
		
		if (!listeners.contains(cb))
			listeners.add(cb);
	}
	
	public synchronized void removeClientCallback(ComCallback cb) {
		
		if (listeners.contains(cb))
			listeners.remove(cb);
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
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Bukkit.getScheduler().cancelTask(taskID);
	}

	@Override
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
				
				listener.communicate(serverIP, data, outData);
				
			} catch (Exception ex) {
				new ClientCommunicationException(serverIP, ex).printStackTrace();
			}
		}
			
		this.write(outData.toByteArray());
	}
	
	public class ClientCommunicationException extends Exception {

		private static final long serialVersionUID = 1L;
		
		public ClientCommunicationException(String ip, Throwable cause) {
			
			super("Unhandled exception while communicating with the server " + ip, cause, false, true);
		}
	}
}
