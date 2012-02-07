package me.ellbristow.SimpleSpawn;

import java.io.File;
import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class SimpleSpawn extends JavaPlugin implements Listener {
	
	public static SimpleSpawn plugin;
	protected FileConfiguration config;
	public FileConfiguration usersConfig = null;
	public File usersFile = null;
	public int tpEffect = 3; // 3 = off

	@Override
	public void onDisable() {
		
	}

	@Override
	public void onEnable() {
		config = getConfig();
		boolean useTpEffect = config.getBoolean("use_teleport_effect", true);
		config.set("use_teleport_effect", useTpEffect);
		if (useTpEffect) {
			tpEffect = config.getInt("teleport_effect_type", 1);
			config.set("teleport_effect_type", tpEffect);
		} else {
			config.set("teleport_effect_type", config.getInt("teleport_effect_type", 1));
		}
		saveConfig();
		usersConfig = this.getUsersConfig();
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(this, this);
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
				usersConfig.set(world + ".x", (double)player.getLocation().getX());
				usersConfig.set(world + ".y", (double)player.getLocation().getY());
				usersConfig.set(world + ".z", (double)player.getLocation().getZ());
				usersConfig.set(world + ".yaw", (double)player.getLocation().getYaw());
				usersConfig.set(world + ".pitch", (double)player.getLocation().getPitch());
				saveUsersConfig();
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
				double spawnX = usersConfig.getDouble(world + ".x", player.getWorld().getSpawnLocation().getX());
				double spawnY = usersConfig.getDouble(world + ".y", player.getWorld().getSpawnLocation().getY());
				double spawnZ = usersConfig.getDouble(world + ".z", player.getWorld().getSpawnLocation().getZ());
				float spawnYaw = (float)usersConfig.getDouble(world + ".yaw", player.getWorld().getSpawnLocation().getYaw());
				float spawnPitch = (float)usersConfig.getDouble(world + ".pitch", player.getWorld().getSpawnLocation().getPitch());
				Location spawnLoc = new Location(player.getWorld(), spawnX, spawnY, spawnZ, spawnYaw, spawnPitch);
				simpleTeleport(player, spawnLoc);
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
				setHomeLoc(player);
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
				Location homeLoc = getHomeLoc(player);
				simpleTeleport(player, homeLoc);
				return true;
			}
		}
		return false;
	}
	
	public void simpleTeleport(Player player, Location loc) {
		Location leftLoc = player.getLocation();
		switch (tpEffect) {
		case 0:
			player.getWorld().strikeLightningEffect(leftLoc);
			loc.getWorld().strikeLightningEffect(loc);
		break;
		default:
			leftLoc.setY(leftLoc.getY() + 1);
			Location newLoc = new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
			newLoc.setY(newLoc.getY() + 1);
			switch (tpEffect) {
			case 1:
				leftLoc.getWorld().playEffect(leftLoc, Effect.ENDER_SIGNAL, 0);
				newLoc.getWorld().playEffect(newLoc, Effect.ENDER_SIGNAL, 0);
			break;
			case 2:
				leftLoc.getWorld().playEffect(leftLoc, Effect.SMOKE, 0);
				newLoc.getWorld().playEffect(newLoc, Effect.SMOKE, 0);
			break;
			case 3:
				leftLoc.getWorld().playEffect(leftLoc, Effect.MOBSPAWNER_FLAMES, 0);
				newLoc.getWorld().playEffect(newLoc, Effect.MOBSPAWNER_FLAMES, 0);
			break;
			}
		break;
		}
		player.teleport(loc);
		player.sendMessage(ChatColor.GOLD + "WHOOSH!");
	}
	
	public void setHomeLoc(Player player) {
		Location homeLoc = player.getLocation();
		homeLoc.setYaw(player.getLocation().getYaw());
		homeLoc.setPitch(player.getLocation().getPitch());
		player.setBedSpawnLocation(homeLoc);
		usersConfig.set(player.getName().toLowerCase() + ".world", homeLoc.getWorld().getName());
		usersConfig.set(player.getName().toLowerCase() + ".x", homeLoc.getX());
		usersConfig.set(player.getName().toLowerCase() + ".y", homeLoc.getY());
		usersConfig.set(player.getName().toLowerCase() + ".z", homeLoc.getZ());
		usersConfig.set(player.getName().toLowerCase() + ".yaw", homeLoc.getYaw());
		usersConfig.set(player.getName().toLowerCase() + ".pitch", homeLoc.getPitch());
		saveUsersConfig();
		player.sendMessage(ChatColor.GOLD + "Your home has been set to this location!");
	}
	
	public Location getHomeLoc(Player player) {
		World homeWorld = getServer().getWorld(usersConfig.getString(player.getName().toLowerCase() + ".world", player.getWorld().getName()));
		double homeX = usersConfig.getDouble(player.getName().toLowerCase() + ".x", player.getWorld().getSpawnLocation().getX());
		double homeY = usersConfig.getDouble(player.getName().toLowerCase() + ".y", player.getWorld().getSpawnLocation().getY());
		double homeZ = usersConfig.getDouble(player.getName().toLowerCase() + ".z", player.getWorld().getSpawnLocation().getZ());
		float homeYaw = (float)usersConfig.getDouble(player.getName().toLowerCase() + ".yaw", player.getWorld().getSpawnLocation().getYaw());
		float homePitch = (float)usersConfig.getDouble(player.getName().toLowerCase() + ".pitch", player.getWorld().getSpawnLocation().getPitch());
		Location homeLoc = new Location(homeWorld, homeX, homeY, homeZ, homeYaw, homePitch);
		return homeLoc;
	}
	
	public void loadUsersConfig() {
		if (usersFile == null) {
			usersFile = new File(getDataFolder(),"locations.yml");
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
			getLogger().severe("Could not save " + usersFile + "!");
		}
	}
	
	/* EVENT LISTENERS */
	
	@EventHandler (priority = EventPriority.NORMAL)
	public void onPlayerRespawn (PlayerRespawnEvent event) {
		Location homeLoc = getHomeLoc(event.getPlayer());
		event.setRespawnLocation(homeLoc);
	}
	
	@EventHandler (priority = EventPriority.NORMAL)
	public void onPlayerBedEnter (PlayerBedEnterEvent event) {
		setHomeLoc(event.getPlayer());
	}
}
