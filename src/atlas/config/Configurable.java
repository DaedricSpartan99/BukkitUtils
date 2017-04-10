package atlas.config;

import org.bukkit.configuration.ConfigurationSection;

public interface Configurable {

	String getConfigurationSection();
	
	void save(ConfigurationSection section);
	boolean load(ConfigurationSection section);
}
