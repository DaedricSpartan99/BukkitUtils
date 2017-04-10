package atlas.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.List;

public class BukkitFiles {

	public static void copy(File srcFile, String destName, File destDir, List<String> ignore) {
		
		if (ignore.contains(srcFile.getName()))
			return;
		
		File destFile = new File(destDir.getAbsolutePath() + "/" + destName);
		
		if (srcFile.isDirectory()) {
			
			if (!destFile.exists())
				destFile.mkdir();
			
			for (File file : srcFile.listFiles())
				copy(file, file.getName(), destFile, ignore);
			
			return;
		}
		
		try {
			
			if (destFile.exists())
				destFile.delete();
			
			destFile.createNewFile();
			
		} catch (IOException ex) {
			ex.printStackTrace();
			return;
		}

	    FileChannel source = null;
	    FileChannel destination = null;

	    try {
	    	
	        source = new FileInputStream(srcFile).getChannel();
	        destination = new FileOutputStream(destFile).getChannel();
	        destination.transferFrom(source, 0, source.size());
	        
	    } catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
			
		} finally {
	    	
	    	try {
	    		
	    		if(source != null)
	    			source.close();
	    		
	    		if(destination != null)
	    			destination.close();
	    		
	    	} catch (IOException ex) {
	    		ex.printStackTrace();
	    		return;
	    	}
	        
	        System.out.println(srcFile.getName() + " copied successfully");
	    }
	}
	
	public static void recursiveRemove(File src) {
		
		if (!src.exists())
			return;
		
		if (src.isDirectory()) {
			
			for (File child : src.listFiles())
				recursiveRemove(child);
		}
		
		src.delete();
	}
}
