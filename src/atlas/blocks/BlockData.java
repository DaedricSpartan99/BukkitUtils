package atlas.blocks;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.util.Vector;

public class BlockData implements ConfigurationSerializable {
	
	public final Vector location;
	public final int material;
	public final byte data;
	
	public BlockData(Vector location, int material, byte data) {
		
		this.location = location;
		this.material = material;
		this.data = data;
	}
	
	@SuppressWarnings("deprecation")
	public BlockData(Vector location, Material material, byte data) {
		
		this.location = location;
		this.material = material.getId();
		this.data = data;
	}
	
	@SuppressWarnings("deprecation")
	public BlockData(Block block) {
		
		this.location = block.getLocation().toVector();
		this.material = block.getType().getId();
		this.data = block.getData();
	}
	
	@SuppressWarnings("deprecation")
	public boolean isSet(World world) {
		
		Block block = location.toLocation(world).getBlock();
		return block.getType().equals(material) && block.getData() == data;
	}
	
	public Block setBlock(World world) {
		
		return this.setBlock(location.toLocation(world).getBlock());
	}
	
	@SuppressWarnings("deprecation")
	public void erase(World world) {
		
		Block block = location.toLocation(world).getBlock();
		block.setTypeIdAndData(0, (byte)0, false);
	} 
	
	public void breakNaturally(World world) {
		
		location.toLocation(world).getBlock().breakNaturally();
	}
	
	public Block setBlock(Location phase) {
		
		return setBlock(phase.clone().add(location).getBlock());
	}
	
	@SuppressWarnings("deprecation")
	public Block setBlock(Block block) {
		
		if (block.getTypeId() == material && block.getData() == data)
			return block;
		
		block.setTypeIdAndData(material, data, false);
		
		return block;
	}
	
	public void erase(Location phase) {
		
		erase(phase.clone().add(location).getBlock());
	}
	
	@SuppressWarnings("deprecation")
	public static void erase(Block block) {
		
		switch (block.getType()) {
		
		case STATIONARY_WATER:
			removeWaterFlow(block);
			break;
		case STATIONARY_LAVA:
			removeLavaFlow(block);
			break;
		default:
			block.setTypeIdAndData(0, (byte)0, false);
			break;
		}
	}
	
	public void breakNaturally(Location phase, boolean drop) {
		
		Block block = phase.clone().add(location).getBlock();
		
		if (!drop)
			block.getDrops().clear();
		
		block.breakNaturally();
	}
	
	public static final BlockFace[] FLOW_FACES = {BlockFace.NORTH, 
												BlockFace.SOUTH, 
												BlockFace.EAST, 
												BlockFace.WEST, 
												BlockFace.DOWN};
	
	@SuppressWarnings("deprecation")
	public static void removeWaterFlow(Block block) {
		
		block.setTypeIdAndData(0, (byte)0, false);
		
		for (BlockFace face : FLOW_FACES) {
			
			Block side = block.getRelative(face);
			
			if (side.getType() == Material.WATER)
				removeWaterFlow(side);
		}
	}
	
	@SuppressWarnings("deprecation")
	public static void removeLavaFlow(Block block) {
		
		block.setTypeIdAndData(0, (byte)0, false);
		
		for (BlockFace face : FLOW_FACES) {
			
			Block side = block.getRelative(face);
			
			if (side.getType() == Material.LAVA)
				removeLavaFlow(side);
		}
	}
	
	public static void registerConfiguration() {
		
		ConfigurationSerialization.registerClass(BlockData.class);
	}
	
	// string format IO

	@Override
	public Map<String, Object> serialize() {
		
		Map<String, Object> map = new HashMap<String, Object>();
		
		map.put("location", location);
		map.put("material", material);
		map.put("data", (int)data);
		
		return map;
	}
	
	@SuppressWarnings("deprecation")
	public static BlockData deserialize(Map<String, Object> map) {
		
		if (!(map.containsKey("location") && map.containsKey("material") && map.containsKey("data")))
			return null;
		
		Vector location = null;
		int material;
		byte data;
		
		if (map.get("location") instanceof Vector)
			location = (Vector) map.get("location");
		
		if (map.get("material") instanceof Integer)
			material = (Integer) map.get("material");	
		else
			return null;
		
		if (map.get("data") instanceof Byte)
			data = (Byte) map.get("data");
		else
			return null;
		
		if (location == null)
			return null;
		
		if (material == Material.CHEST.getId())
			return new ChestData(location, data);
		else
			return new BlockData(location, material, data);
	}
	
	// binary format IO
	
	public static final int BIN_SIZE = 17;
	
	public void write(DataOutputStream stream) throws IOException {
		
		stream.writeInt(location.getBlockX());
		stream.writeInt(location.getBlockY());
		stream.writeInt(location.getBlockZ());
		stream.writeInt(material);
		stream.writeByte(data);
	}
	
	@SuppressWarnings("deprecation")
	public static BlockData read(DataInputStream stream) throws IOException {
		
		int x, y, z, material;
		byte data;
		
		x = stream.readInt();
		y = stream.readInt();
		z = stream.readInt();
		material = stream.readInt();
		data = stream.readByte();
		
		if (material == Material.CHEST.getId())
			return new ChestData(new Vector(x, y, z), data);
		
		return new BlockData(new Vector(x, y, z), material, data);
	}
	
	public static void writeBlocks(OutputStream stream, BlockData[] blocks) {
		
		DataOutputStream dataStream = new DataOutputStream(stream);
		
		try {
			
			for (int i = 0; i < blocks.length; i++)
				blocks[i].write(dataStream);
			
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			
			try {
				dataStream.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			
			try {
				stream.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	public static BlockData[] loadBlocks(InputStream stream) {
		
		Collection<BlockData> list = new ArrayList<BlockData>();
		DataInputStream dataStream = new DataInputStream(stream);
			
		try {
			
			while(true) {
				
				try {
					list.add(BlockData.read(dataStream));
				} catch (EOFException ex) {
					break;
				}
			}
			
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		} finally {
			
			try {
				dataStream.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			
			try {
				stream.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		
		return list.toArray(new BlockData[list.size()]);
	}
}
