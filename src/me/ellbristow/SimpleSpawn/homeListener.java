package me.ellbristow.SimpleSpawn;

import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class homeListener implements Listener {
	
	public static SimpleSpawn plugin;
	public final Logger logger = Logger.getLogger("Minecraft");
	
	public homeListener (SimpleSpawn instance) {
		plugin = instance;
	}
	
	@EventHandler (priority = EventPriority.NORMAL)
	public void onPlayerRespawn (PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		World homeWorld = plugin.getServer().getWorld(plugin.usersConfig.getString(player.getName().toLowerCase() + ".world", plugin.getServer().getWorlds().get(0).getName()));
		double homeX = plugin.usersConfig.getDouble(player.getName().toLowerCase() + ".x", player.getWorld().getSpawnLocation().getX());
		double homeY = plugin.usersConfig.getDouble(player.getName().toLowerCase() + ".y", player.getWorld().getSpawnLocation().getY());
		double homeZ = plugin.usersConfig.getDouble(player.getName().toLowerCase() + ".z", player.getWorld().getSpawnLocation().getZ());
		float homeYaw = (float)plugin.usersConfig.getDouble(player.getName().toLowerCase() + ".yaw", player.getWorld().getSpawnLocation().getYaw());
		float homePitch = (float)plugin.usersConfig.getDouble(player.getName().toLowerCase() + ".pitch", player.getWorld().getSpawnLocation().getPitch());
		Location spawnLoc = new Location(homeWorld, homeX, homeY, homeZ, homeYaw, homePitch);
		event.setRespawnLocation(spawnLoc);
	}
	
	@EventHandler (priority = EventPriority.NORMAL)
	public void onPlayerBedEnter (PlayerBedEnterEvent event) {
		Player player = event.getPlayer();
		plugin.usersConfig.set(player.getName().toLowerCase() + ".world", player.getWorld().getName());
		plugin.usersConfig.set(player.getName().toLowerCase() + ".x", (int) player.getLocation().getX());
		plugin.usersConfig.set(player.getName().toLowerCase() + ".y", (int) player.getLocation().getY());
		plugin.usersConfig.set(player.getName().toLowerCase() + ".z", (int) player.getLocation().getZ());
		plugin.usersConfig.set(player.getName().toLowerCase() + ".yaw", (int) player.getLocation().getYaw());
		plugin.usersConfig.set(player.getName().toLowerCase() + ".pitch", (int) player.getLocation().getPitch());
		plugin.saveUsersConfig();
		player.sendMessage(ChatColor.GOLD + "Your home has been set to this location!");
	}
	
}
