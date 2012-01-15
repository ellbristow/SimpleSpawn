package me.ellbristow.SimpleSpawn;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class SimpleSpawn extends JavaPlugin {
	
	public static SimpleSpawn plugin;
	public final Logger logger = Logger.getLogger("minecraft");
	public final homeListener homeListener = new homeListener(this);
	public FileConfiguration usersConfig = null;
	public File usersFile = null;

	@Override
	public void onDisable() {
		PluginDescriptionFile pdfFile = this.getDescription();
		logger.info("[" + pdfFile.getName() + "] is disabled.");		
	}

	@Override
	public void onEnable() {
		PluginDescriptionFile pdfFile = this.getDescription();
		logger.info("[" + pdfFile.getName() + "] version " + pdfFile.getVersion() + " is now enabled.");
		usersConfig = this.getUsersConfig();
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_RESPAWN, homeListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_BED_ENTER, homeListener, Event.Priority.Normal, this);
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (commandLabel.equalsIgnoreCase("setspawn")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage("Sorry! This command cannot be run from the console!");
				return false;
			}
			Player player = (Player) sender;
			if (player.hasPermission("simplespawn.set")) {
				String world = player.getWorld().getName();
				getServer().getWorld(world).setSpawnLocation((int)player.getLocation().getX(), (int)player.getLocation().getY(), (int)player.getLocation().getZ());
				player.sendMessage(ChatColor.GOLD + "Spawn been set to this location for this world!");
				return true;
			}
		}
		else if (commandLabel.equalsIgnoreCase("spawn")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage("Sorry! This command cannot be run from the console!");
				return false;
			}
			Player player = (Player) sender;
			if (player.hasPermission("simplespawn.use")) {
				String world = player.getWorld().getName();
				Location spawnLoc = getServer().getWorld(world).getSpawnLocation();
				player.teleport(spawnLoc);
				player.sendMessage(ChatColor.GOLD + "WHOOSH!");
				return true;
			}
		}
		else if (commandLabel.equalsIgnoreCase("sethome")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage("Sorry! This command cannot be run from the console!");
				return false;
			}
			Player player = (Player) sender;
			if (player.hasPermission("simplespawn.home.set")) {
				usersConfig.set(player.getName().toLowerCase() + ".world", player.getWorld().getName());
				usersConfig.set(player.getName().toLowerCase() + ".x", player.getLocation().getX());
				usersConfig.set(player.getName().toLowerCase() + ".y", player.getLocation().getY());
				usersConfig.set(player.getName().toLowerCase() + ".z", player.getLocation().getZ());
				saveUsersConfig();
				player.sendMessage(ChatColor.GOLD + "Your home has been set to this location!");
				return true;
			}
		}
		else if (commandLabel.equalsIgnoreCase("home")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage("Sorry! This command cannot be run from the console!");
				return false;
			}
			Player player = (Player) sender;
			if (player.hasPermission("simplespawn.home.use")) {
				World homeWorld = getServer().getWorld(usersConfig.getString(player.getName().toLowerCase() + ".world", getServer().getWorlds().get(0).getName()));
				double homeX = usersConfig.getDouble(player.getName().toLowerCase() + ".x", player.getWorld().getSpawnLocation().getX());
				double homeY = usersConfig.getDouble(player.getName().toLowerCase() + ".y", player.getWorld().getSpawnLocation().getY());
				double homeZ = usersConfig.getDouble(player.getName().toLowerCase() + ".z", player.getWorld().getSpawnLocation().getZ());
				Location homeLoc = new Location(homeWorld, homeX, homeY, homeZ);
				player.teleport(homeLoc);
				player.sendMessage(ChatColor.GOLD + "WHOOSH!");
				return true;
			}
		}
		return false;
	}
	
	public void loadUsersConfig() {
		if (usersFile == null) {
			usersFile = new File(getDataFolder(),"users.yml");
		}
		usersConfig = YamlConfiguration.loadConfiguration(usersFile);
	}
	
	public FileConfiguration getUsersConfig() {
		if (usersConfig == null) {
			loadUsersConfig();
		}
		return usersConfig;
	}
	
	public void saveUsersConfig() {
		if (usersConfig == null || usersFile == null) {
			return;
		}
		try {
			usersConfig.save(usersFile);
		} catch (IOException ex) {
			logger.log(Level.SEVERE, "Could not save " + this.usersFile, ex );
		}
	}
}
