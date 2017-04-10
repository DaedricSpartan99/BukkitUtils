package atlas.blocks;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.util.Vector;

public class ChestData extends BlockData {

	public ChestData(Vector location, byte data) {
		super(location,Material.CHEST, data);
	}
	
	public Inventory getInventory(World world) {
		
		Block block;
		
		if (super.isSet(world))
			block = location.toLocation(world).getBlock();
		else
			block = super.setBlock(world);
		
		
		if (block.getState() instanceof Chest)
			return ((Chest)block.getState()).getBlockInventory();
		else
			return null;
	}
}
