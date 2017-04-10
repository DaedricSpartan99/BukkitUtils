package atlas.blocks;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.util.Vector;

public class RawLocation implements ConfigurationSerializable {
	
	public String world;
	public Vector vector;
	public float yaw, pitch;
	
	public RawLocation(Location location) {
		
		this.world = location.getWorld().getName();
		this.vector = location.toVector();
		this.yaw = location.getYaw();
		this.pitch = location.getPitch();
	}
	
	public RawLocation(String world, Vector vector, float yaw, float pitch) {
		
		this.world = world;
		this.vector = vector;
		this.yaw = yaw;
		this.pitch = pitch;
	}
	
	public RawLocation(String world, Vector vector) {
		
		this.world = world;
		this.vector = vector;
		this.yaw = 0;
		this.pitch = 0;
	}
	
	public Location getLocation() {
		
		World w = Bukkit.getWorld(world);
		
		if (w == null)
			return null;
		
		return new Location(w, vector.getX(), vector.getY(), vector.getZ(), yaw, pitch);
	}

	@Override
	public Map<String, Object> serialize() {
		
		Map<String, Object> map = new HashMap<String, Object>();
		
		map.put("world", world);
		map.put("vector", vector);
		map.put("yaw", (double)yaw);
		map.put("pitch", (double)pitch);

		return map;
	}
	
	public static void registerConfiguration() {
		
		ConfigurationSerialization.registerClass(RawLocation.class);
	}

	public static RawLocation deserialize(Map<String, Object> map) {
		
		Object _world = map.get("world"),
				_vector = map.get("vector"),
				_yaw = map.get("yaw"),
				_pitch = map.get("pitch");
		
		String world;
		
		if (_world instanceof String)
			world = (String)_world;
		else
			world = "world";
		
		Vector vector;
		
		if (_vector instanceof Vector)
			vector = (Vector)_vector;
		else
			vector = new Vector(0, 64, 0);
		
		float yaw, pitch;
		
		if (_yaw instanceof Double)
			yaw = ((Double)_yaw).floatValue();
		else
			yaw = 0;
		
		if (_pitch instanceof Double)
			pitch = ((Double)_pitch).floatValue();
		else
			pitch = 0;
		
		return new RawLocation(world, vector, yaw, pitch);
	}
}
