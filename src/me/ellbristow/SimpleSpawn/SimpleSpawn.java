package me.ellbristow.SimpleSpawn;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class SimpleSpawn extends JavaPlugin implements Listener {
	
    public static SimpleSpawn plugin;
    protected FileConfiguration config;
    private SQLBridge SSdb;
    public int tpEffect = 4; // 4 = off
    private String[] spawnColumns = {"world", "x", "y", "z", "yaw", "pitch"};
    private String[] spawnDims = {"TEXT NOT NULL PRIMARY KEY", "DOUBLE NOT NULL DEFAULT 0", "DOUBLE NOT NULL DEFAULT 0", "DOUBLE NOT NULL DEFAULT 0", "FLOAT NOT NULL DEFAULT 0", "FLOAT NOT NULL DEFAULT 0"};
    private String[] homeColumns = {"player", "world", "x", "y", "z", "yaw", "pitch"};
    private String[] homeDims = {"TEXT NOT NULL PRIMARY KEY", "TEXT NOT NULL", "DOUBLE NOT NULL DEFAULT 0", "DOUBLE NOT NULL DEFAULT 0", "DOUBLE NOT NULL DEFAULT 0", "FLOAT NOT NULL DEFAULT 0", "FLOAT NOT NULL DEFAULT 0"};
    private String[] jailColumns = {"name", "world", "x", "y", "z", "yaw", "pitch"};
    private String[] jailDims = {"TEXT NOT NULL PRIMARY KEY", "TEXT NOT NULL", "DOUBLE NOT NULL DEFAULT 0", "DOUBLE NOT NULL DEFAULT 0", "DOUBLE NOT NULL DEFAULT 0", "FLOAT NOT NULL DEFAULT 0", "FLOAT NOT NULL DEFAULT 0"};
    private String[] jailedColumns = {"player","jailName"};
    private String[] jailedDims = {"TEXT NOT NULL PRIMARY KEY", "TEXT NOT NULL"};

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
        SSdb = new SQLBridge(this);
        SSdb.getConnection();
        if (new File(getDataFolder(),"locations.yml").exists()) {
            convertDb();
        }
        if (!SSdb.checkTable("Jails")) {
            SSdb.createTable("Jails", jailColumns, jailDims);
        }
        if (!SSdb.checkTable("Jailed")) {
            SSdb.createTable("Jailed", jailedColumns, jailedDims);
        }
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Sorry! This command cannot be run from the console!");
            return false;
        }
        Player player = (Player) sender;
        if (commandLabel.equalsIgnoreCase("setspawn")) {
            if (!player.hasPermission("simplespawn.set")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to set the spawn location!");
                return false;
            }
            if (args.length == 0) {
                setWorldSpawn(player.getLocation());
                player.sendMessage(ChatColor.GOLD + "Spawn been set to this location for this world!");
                return true;
            } else if (args.length == 1) {
                if (args[0].equalsIgnoreCase("ssdefault")) {
                    setDefaultSpawn(player.getLocation());
                    player.sendMessage(ChatColor.GOLD + "Spawn been set to this location for NEW players!");
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "Command not recognised!");
                    player.sendMessage(ChatColor.RED + "Try: /setspawn OR /setspawn SSdefault");
                    return false;
                }
            } else {
                player.sendMessage(ChatColor.RED + "Command not recognised!");
                player.sendMessage(ChatColor.RED + "Try: /setspawn OR /setspawn SSdefault");
                return false;
            }
        } else if (commandLabel.equalsIgnoreCase("spawn")) {
            if (!player.hasPermission("simplespawn.use")) {
                if (!player.hasPermission("simplespawn.use.default")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
                    return false;
                }
                simpleTeleport(player, getDefaultSpawn());
                return true;
            }
            if (args.length == 0) {
                simpleTeleport(player, getWorldSpawn(player.getWorld().getName()));
                return true;
            } else if (args.length == 1)  {
                World world = getServer().getWorld(args[0]);
                if (world == null) {
                    player.sendMessage(ChatColor.RED + "World '"+ ChatColor.WHITE + args[0] + ChatColor.RED +"' not found!");
                    return false;
                }
                simpleTeleport(player, getWorldSpawn(world.getName()));
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "Command not recognised!");
                player.sendMessage(ChatColor.RED + "Try: /spawn {worldName}");
                return false;
            }
        } else if (commandLabel.equalsIgnoreCase("sethome")) {
            if (args.length == 0) {
                if (!player.hasPermission("simplespawn.home.set")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
                    return false;
                }
                setHomeLoc(player);
                player.sendMessage(ChatColor.GOLD + "Your home has been set to this location!");
                return true;
            } else if (args.length == 1) {
                if (!player.hasPermission("simplespawn.home.set.others")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to set other peoples home!");
                    return false;
                }
                OfflinePlayer target = getServer().getOfflinePlayer(args[0]);
                if (!target.hasPlayedBefore()) {
                    player.sendMessage(ChatColor.RED + "Player '" + ChatColor.WHITE + args[0] + ChatColor.RED + "' not found!");
                    return false;
                }
                setOtherHomeLoc(target, player);
                player.sendMessage(target.getName() + ChatColor.GOLD + "'s home has been set to this location!");
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "Command not recognised!");
                player.sendMessage(ChatColor.RED + "Try: /sethome OR /sethome {playerName}");
                return false;
            }
        } else if (commandLabel.equalsIgnoreCase("home")) {
            if (!player.hasPermission("simplespawn.home.use")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
                return false;
            }
            Location homeLoc = getHomeLoc(player);
            simpleTeleport(player, homeLoc);
            return true;
        } else if (commandLabel.equalsIgnoreCase("setjail")) {
            if (!player.hasPermission("simplespawn.jail.set")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
                return false;
            }
            if (args.length == 0) {
                setJail("default", player.getLocation());
                player.sendMessage(ChatColor.GOLD + "Default jail set to your location!");
            } else if (args.length == 1) {
                setJail(args[0], player.getLocation());
                player.sendMessage(ChatColor.GOLD + "Jail '" + ChatColor.WHITE + args[0] + ChatColor.GOLD + "' set to your location!");
            } else {
                player.sendMessage(ChatColor.RED + "Command not recognised!");
                player.sendMessage(ChatColor.RED + "Try: /setjail OR /setjail {jailName}");
                return false;
            }
        } else if (commandLabel.equalsIgnoreCase("jail")) {
            if (!player.hasPermission("simplespawn.jail.use")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
                return false;
            }
            if (args.length == 1) {
                if (getJail("default") == null) {
                    player.sendMessage(ChatColor.RED + "No default jail has been set!");
                    player.sendMessage(ChatColor.RED + "Use: /setjail");
                    return false;
                }
                if (args[0].equalsIgnoreCase(player.getName())) {
                    player.sendMessage(ChatColor.RED + "You can't jail yourself!");
                    return false;
                }
                OfflinePlayer target = getServer().getOfflinePlayer(args[0]);
                if (!target.hasPlayedBefore()) {
                    player.sendMessage(ChatColor.RED + "Player '" + ChatColor.WHITE + args[0] + ChatColor.RED + "' not found!");
                    return false;
                }
                if (target.isOnline()) {
                    Player targetPlayer = target.getPlayer();
                    if (!targetPlayer.hasPermission("simplespawn.jail.immune")) {
                        targetPlayer.teleport(getJail("default"), TeleportCause.PLUGIN);
                        setJailed(target.getName(), true, "default");
                        getServer().broadcastMessage(target.getName() + ChatColor.GOLD + " has been jailed!");
                        return true;
                    } else {
                        player.sendMessage(targetPlayer.getName() + ChatColor.RED + " cannot be jailed!");
                        return false;
                    }
                } else {
                    setJailed(target.getName(), true, "default");
                    getServer().broadcastMessage(target.getName() + ChatColor.GOLD + " has been sentenced to jail!");
                }
                return true;
            } else if (args.length == 2) {
                if (getJail(args[1]) == null) {
                    player.sendMessage(ChatColor.RED + "Could not find a jail called '" + ChatColor.WHITE + args[1] + ChatColor.RED + "'!");
                    player.sendMessage(ChatColor.RED + "Use: /jail [player] {jailName}");
                    return false;
                }
                if (args[0].equalsIgnoreCase(player.getName())) {
                    player.sendMessage(ChatColor.RED + "You can't jail yourself!");
                    return false;
                }
                OfflinePlayer target = getServer().getOfflinePlayer(args[0]);
                if (!target.hasPlayedBefore()) {
                    player.sendMessage(ChatColor.RED + "Player '" + ChatColor.WHITE + args[0] + ChatColor.RED + "' not found!");
                    return false;
                }
                if (target.isOnline()) {
                    Player targetPlayer = target.getPlayer();
                    if (!targetPlayer.hasPermission("simplespawn.jail.immune")) {
                        targetPlayer.teleport(getJail(args[1]), TeleportCause.PLUGIN);
                        setJailed(target.getName(), true, args[1]);
                        getServer().broadcastMessage(target.getName() + ChatColor.GOLD + " has been jailed!");
                        return true;
                    } else {
                        player.sendMessage(targetPlayer.getName() + ChatColor.RED + " cannot be jailed!");
                        return false;
                    }
                } else {
                    setJailed(target.getName(), true, "default");
                    getServer().broadcastMessage(target.getName() + ChatColor.GOLD + " has been sentenced to jail!");
                }
            } else {
                player.sendMessage(ChatColor.RED + "Command not recognised!");
                player.sendMessage(ChatColor.RED + "Try: /jail [playerName] {jailName}");
                return false;
            }
        } else if (commandLabel.equalsIgnoreCase("release")) {
            if (!player.hasPermission("simplespawn.jail.release")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
                return false;
            }
            if (args.length == 1) {
                OfflinePlayer target = getServer().getOfflinePlayer(args[0]);
                if (!target.hasPlayedBefore()) {
                    player.sendMessage(ChatColor.RED + "Player '" + ChatColor.WHITE + args[0] + ChatColor.RED + "' not found!");
                    return false;
                }
                if (!isJailed(target.getName())) {
                    player.sendMessage(target.getName() + ChatColor.RED + " is not in jail!");
                    return false;
                }
                setJailed(target.getName(), false, args[0]);
                getServer().broadcastMessage(target.getName() + ChatColor.GOLD + " has been released from jail!");
                if (target.isOnline()) {
                    target.getPlayer().sendMessage(ChatColor.GOLD + "You may now leave the jail!");
                }
            } else {
                player.sendMessage(ChatColor.RED + "Command not recognised!");
                player.sendMessage(ChatColor.RED + "Try: /release [playerName]");
                return false;
            }
        } else if (commandLabel.equalsIgnoreCase("jails")) {
            if (!player.hasPermission("simplespawn.jail.list")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
                return false;
            }
            HashMap<Integer, HashMap<String, Object>> jailList = SSdb.select("name", "Jails",null,null,null);
            if (jailList == null || jailList.isEmpty()) {
                player.sendMessage(ChatColor.RED + "No jails were found!");
                return true;
            }
            String jailNames = "";
            for (int i = 0; i < jailList.size(); i++) {
                if (i!=0) {
                    jailNames += ",";
                }
                jailNames += jailList.get(i).get("name");
            }
            String[] jailSplit = jailNames.split(",");
            String jails = ChatColor.GOLD + "";
            for (int i = 0; i < jailSplit.length; i++) {
                if (i!=0) {
                    jails += ChatColor.GOLD + ", ";
                }
                String jail = jailSplit[i];
                jails += jail;
                HashMap<Integer, HashMap<String, Object>> inMates = SSdb.select("player", "Jailed", "jailName = '" + jail + "'", null, null);
                if (inMates != null && !inMates.isEmpty()) {
                    jails += ChatColor.GRAY + " (Inmates: " + inMates.size() + ")";
                }
            }
            player.sendMessage(jails);
        }
        return false;
    }

    public void simpleTeleport(Player player, Location loc) {
        Location leftLoc = player.getLocation();
        switch (tpEffect) {
        case 0:
            player.getWorld().strikeLightningEffect(leftLoc);
        break;
        default:
            leftLoc.setY(leftLoc.getY() + 1);
            switch (tpEffect) {
            case 1:
                leftLoc.getWorld().playEffect(leftLoc, Effect.ENDER_SIGNAL, 0);
            break;
            case 2:
                leftLoc.getWorld().playEffect(leftLoc, Effect.SMOKE, 0);
            break;
            case 3:
                leftLoc.getWorld().playEffect(leftLoc, Effect.MOBSPAWNER_FLAMES, 0);
            break;
            }
        break;
        }
        player.teleport(loc);
        switch (tpEffect) {
        case 0:
            loc.getWorld().strikeLightningEffect(loc);
        break;
        default:
            Location newLoc = new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
            newLoc.setY(newLoc.getY() + 1);
            switch (tpEffect) {
            case 1:
                newLoc.getWorld().playEffect(newLoc, Effect.ENDER_SIGNAL, 0);
            break;
            case 2:
                newLoc.getWorld().playEffect(newLoc, Effect.SMOKE, 0);
            break;
            case 3:
                newLoc.getWorld().playEffect(newLoc, Effect.MOBSPAWNER_FLAMES, 0);
            break;
            }
        break;
        }
        player.sendMessage(ChatColor.GOLD + "WHOOSH!");
    }

    public void setHomeLoc(Player player) {
            Location homeLoc = player.getLocation();
            String world = homeLoc.getWorld().getName();
            double x = homeLoc.getX();
            double y = homeLoc.getY();
            double z = homeLoc.getZ();
            float yaw = homeLoc.getYaw();
            float pitch = homeLoc.getPitch();
            SSdb.query("INSERT OR REPLACE INTO PlayerHomes (player, world, x, y, z, yaw, pitch) VALUES ('" + player.getName() + "', '" + world + "', " + x + ", " + y + ", " + z + ", " + yaw + ", " + pitch + ")");
            player.setBedSpawnLocation(homeLoc);
    }

    public void setOtherHomeLoc(OfflinePlayer target, Player player) {
            Location homeLoc = player.getLocation();
            String world = homeLoc.getWorld().getName();
            double x = homeLoc.getX();
            double y = homeLoc.getY();
            double z = homeLoc.getZ();
            float yaw = homeLoc.getYaw();
            float pitch = homeLoc.getPitch();
            SSdb.query("INSERT OR REPLACE INTO PlayerHomes (player, world, x, y, z, yaw, pitch) VALUES ('" + target.getName() + "', '" + world + "', " + x + ", " + y + ", " + z + ", " + yaw + ", " + pitch + ")");
            player.setBedSpawnLocation(homeLoc);
    }

    public Location getHomeLoc(Player player) {
        HashMap<Integer, HashMap<String, Object>> result = SSdb.select("world, x, y, z, yaw, pitch", "PlayerHomes", "player = '" + player.getName() + "'", null, null);
        Location location;
        if (result.isEmpty()) {
            location = getServer().getWorlds().get(0).getSpawnLocation();
        } else {
            String world = (String)result.get(0).get("world");
            double x = (Double)result.get(0).get("x");
            double y = (Double)result.get(0).get("y");
            double z = (Double)result.get(0).get("z");
            float yaw = Float.parseFloat(result.get(0).get("yaw").toString());
            float pitch = Float.parseFloat(result.get(0).get("pitch").toString());
            location = new Location(getServer().getWorld(world), x, y, z, yaw, pitch);
        }
        return location;
    }

    public void setJail(String jailName, Location loc) {
        jailName = jailName.toLowerCase();
        String world = loc.getWorld().getName();
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        float yaw = loc.getYaw();
        float pitch = loc.getPitch();
        SSdb.query("INSERT OR REPLACE INTO Jails (name, world, x, y, z, yaw, pitch) VALUES ('" + jailName + "', '" + world + "', " + x + ", " + y + ", " + z + ", " + yaw + ", " + pitch + ")");
    }

    public Location getJail(String jailName) {
        jailName = jailName.toLowerCase();
        HashMap<Integer, HashMap<String, Object>> result = SSdb.select("world, x, y, z, yaw, pitch", "Jails", "name = '" + jailName + "'", null, null);
        Location location;
        if (result.isEmpty()) {
            return null;
        } else {
            String world = (String)result.get(0).get("world");
            double x = (Double)result.get(0).get("x");
            double y = (Double)result.get(0).get("y");
            double z = (Double)result.get(0).get("z");
            float yaw = Float.parseFloat(result.get(0).get("yaw").toString());
            float pitch = Float.parseFloat(result.get(0).get("pitch").toString());
            location = new Location(getServer().getWorld(world), x, y, z, yaw, pitch);
        }
        return location;
    }

    public void setJailed(String playerName, boolean toJail, String jailName) {
        if (jailName == null || "".equals(jailName)) {
            jailName = "default";
        } else {
            jailName = jailName.toLowerCase();
        }
        if (toJail) {
            SSdb.query("INSERT OR REPLACE INTO Jailed (player, jailName) VALUES ('" + playerName + "', '" + jailName + "')");
        } else {
            SSdb.query("DELETE FROM Jailed WHERE player = '" + playerName + "'");
        }
    }

    public boolean isJailed(String playerName) {
        HashMap<Integer, HashMap<String, Object>> jailed = SSdb.select("player", "Jailed", "player = '" + playerName + "'", null, null);
        if (jailed != null && !jailed.isEmpty()) {
            return true;
        }
        return false;
    }

    public String getWhereJailed(String playerName) {
        HashMap<Integer, HashMap<String, Object>> jailed = SSdb.select("jailName", "Jailed", "player = '" + playerName + "'", null, null);
        if (jailed == null || jailed.isEmpty()) {
            return null;
        }
        return (String)jailed.get(0).get("jailName");
    }

    public void setDefaultSpawn(Location location) {
        String world = location.getWorld().getName();
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        float yaw = location.getYaw();
        float pitch = location.getPitch();
        SSdb.query("INSERT OR REPLACE INTO DefaultSpawn (world, x, y, z, yaw, pitch) VALUES ('" + world + "', " + x + ", " + y + ", " + z + ", " + yaw + ", " + pitch + ")");
        location.getWorld().setSpawnLocation((int)x, (int)y, (int)z);
    }

    public Location getDefaultSpawn() {
        HashMap<Integer, HashMap<String, Object>> result = SSdb.select("world, x, y, z, yaw, pitch", "DefaultSpawn", null, null, null);
        Location location;
        if (result.isEmpty()) {
            location = getServer().getWorlds().get(0).getSpawnLocation();
        } else {
            String world = (String)result.get(0).get("world");
            double x = (Double)result.get(0).get("x");
            double y = (Double)result.get(0).get("y");
            double z = (Double)result.get(0).get("z");
            float yaw = Float.parseFloat(result.get(0).get("yaw").toString());
            float pitch = Float.parseFloat(result.get(0).get("pitch").toString());
            location = new Location(getServer().getWorld(world), x, y, z, yaw, pitch);
        }
        return location;
    }

    public void setWorldSpawn(Location location) {
        String world = location.getWorld().getName();
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        float yaw = location.getYaw();
        float pitch = location.getPitch();
        SSdb.query("INSERT OR REPLACE INTO WorldSpawns (world, x, y, z, yaw, pitch) VALUES ('" + world + "', " + x + ", " + y + ", " + z + ", " + yaw + ", " + pitch + ")");
        location.getWorld().setSpawnLocation((int)x, (int)y, (int)z);
    }

    public Location getWorldSpawn(String world) {
        HashMap<Integer, HashMap<String, Object>> result = SSdb.select("x, y, z, yaw, pitch", "WorldSpawns", "world = '" + world + "'", null, null);
        Location location;
        if (result.isEmpty()) {
            location = getServer().getWorld(world).getSpawnLocation();
        } else {
            double x = (Double)result.get(0).get("x");
            double y = (Double)result.get(0).get("y");
            double z = (Double)result.get(0).get("z");
            float yaw = Float.parseFloat(result.get(0).get("yaw").toString());
            float pitch = Float.parseFloat(result.get(0).get("pitch").toString());
            location = new Location(getServer().getWorld(world), x, y, z, yaw, pitch);
        }
        return location;
    }

    private void convertDb() {
        FileConfiguration usersConfig = null;
        File usersFile = null;
        usersFile = new File(getDataFolder(),"locations.yml");
        usersConfig = YamlConfiguration.loadConfiguration(usersFile);
        SSdb.createTable("WorldSpawns", spawnColumns, spawnDims);
        List<World> worlds = getServer().getWorlds();
        for (int i = 0; i < worlds.size(); i++) {
            if (usersConfig.getConfigurationSection(worlds.get(i).getName()) != null) {
                String worldName = worlds.get(i).getName();
                double x = usersConfig.getDouble(worldName + ".x");
                double y = usersConfig.getDouble(worldName + ".y");
                double z = usersConfig.getDouble(worldName + ".z");
                float yaw = Float.parseFloat(usersConfig.get(worldName + ".yaw").toString());
                float pitch = Float.parseFloat(usersConfig.get(worldName + ".pitch").toString());
                String query = "INSERT OR REPLACE INTO WorldSpawns (world, x, y, z, yaw, pitch) VALUES ('" + worldName + "', " + x + ", " + y + ", " + z + ", " + yaw + ", " + pitch + ")";
                SSdb.query(query);
            }
        }
        Location defaultLoc = getServer().getWorlds().get(0).getSpawnLocation();
        SSdb.createTable("DefaultSpawn", spawnColumns, spawnDims);
        setDefaultSpawn(defaultLoc);
        SSdb.createTable("PlayerHomes", homeColumns, homeDims);
        OfflinePlayer[] players = getServer().getOfflinePlayers();
        for (int i = 0; i < players.length; i++) {
            if (usersConfig.getConfigurationSection(players[i].getName()) != null) {
                String playerName = players[i].getName();
                String worldName = usersConfig.getString(playerName + ".world");
                double x = usersConfig.getDouble(playerName + ".x");
                double y = usersConfig.getDouble(playerName + ".y");
                double z = usersConfig.getDouble(playerName + ".z");
                float yaw = Float.parseFloat(usersConfig.get(playerName + ".yaw").toString());
                float pitch = Float.parseFloat(usersConfig.get(playerName + ".pitch").toString());
                String query = "INSERT OR REPLACE INTO PlayerHomes (player, world, x, y, z, yaw, pitch) VALUES ('" + playerName + "', '" + worldName + "', " + x + ", " + y + ", " + z + ", " + yaw + ", " + pitch + ")";
                SSdb.query(query);
            }
        }
        usersFile.delete();
        getLogger().info("locations.yml converted to SQLite SimpleSpawn.db");
    }

    /* EVENT LISTENERS */

    @EventHandler (priority = EventPriority.NORMAL)
    public void onPlayerJoin (PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPlayedBefore()) {
            player.teleport(getWorldSpawn("SSdefault"), TeleportCause.PLUGIN);
            return;
        }
        if (isJailed(player.getName())) {
            if (player.hasPermission("simplespawn.jail.immune")) {
                setJailed(player.getName(), false, null);
                getServer().broadcastMessage(player.getName() + ChatColor.GOLD + " was pardoned from serving jail time!");
            } else {
                player.teleport(getJail(getWhereJailed(player.getName())) ,TeleportCause.PLUGIN);
                getServer().broadcastMessage(player.getName() + ChatColor.GOLD + " has been jailed!");
            }
        }
    }

    @EventHandler (priority = EventPriority.NORMAL)
    public void onPlayerRespawn (PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (isJailed(player.getName())) {
            if (player.hasPermission("simplespawn.jail.immune")) {
                setJailed(player.getName(), false, null);
                getServer().broadcastMessage(player.getName() + ChatColor.GOLD + " was pardoned from serving jail time!");
                event.setRespawnLocation(getHomeLoc(player));
            } else {
                event.setRespawnLocation(getJail(getWhereJailed(player.getName())));
                getServer().broadcastMessage(player.getName() + ChatColor.GOLD + " has been jailed!");
            }
        } else {
            event.setRespawnLocation(getHomeLoc(player));
        }
    }

    /*
    @EventHandler (priority = EventPriority.NORMAL)
    public void onPlayerBedEnter (PlayerBedEnterEvent event) {
            setHomeLoc(event.getPlayer());
    }
    */

    @EventHandler (priority = EventPriority.NORMAL)
    public void onPlayerBedLeave (PlayerBedLeaveEvent event) {
            event.getPlayer().teleport(getHomeLoc(event.getPlayer()), TeleportCause.PLUGIN);
    }

    @EventHandler (priority = EventPriority.NORMAL)
    public void onPlayerInteract (PlayerInteractEvent event) {
        if (!event.isCancelled()) {
            Player player = event.getPlayer();
            if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && event.getClickedBlock().getType().equals(Material.BED_BLOCK)) {
                setHomeLoc(player);
                player.sendMessage(ChatColor.GOLD + "Your home has been set to this location!");
            } else {
                if (isJailed(player.getName())) {
                    if (player.hasPermission("simplespawn.jail.immune")) {
                        setJailed(player.getName(), false, null);
                    } else {
                        event.setCancelled(true);
                        if (!event.getAction().equals(Action.PHYSICAL)) {
                            player.sendMessage(ChatColor.RED + "You cannot interact with the world while in jail!");
                        }
                    }
                }
            }
        }
    }

    @EventHandler (priority = EventPriority.NORMAL)
    public void onPlayerTeleport (PlayerTeleportEvent event) {
        if (!event.isCancelled()) {
            if (event.getCause().equals(TeleportCause.COMMAND)) {
                Player player = event.getPlayer();
                if (isJailed(player.getName())) {
                    if (player.hasPermission("simplespawn.jail.immune")) {
                        setJailed(player.getName(), false, null);
                        getServer().broadcastMessage(player.getName() + ChatColor.GOLD + " was pardoned from serving jail time!");
                    } else {
                        event.setTo(getJail(getWhereJailed(player.getName())));
                        player.sendMessage(ChatColor.RED + "You cannot teleport while you're in jail!");
                    }
                }
                Location leftLoc = event.getFrom();
                switch (tpEffect) {
                case 0:
                    player.getWorld().strikeLightningEffect(leftLoc);
                break;
                default:
                    leftLoc.setY(leftLoc.getY() + 1);
                    switch (tpEffect) {
                    case 1:
                        leftLoc.getWorld().playEffect(leftLoc, Effect.ENDER_SIGNAL, 0);
                    break;
                    case 2:
                        leftLoc.getWorld().playEffect(leftLoc, Effect.SMOKE, 0);
                    break;
                    case 3:
                        leftLoc.getWorld().playEffect(leftLoc, Effect.MOBSPAWNER_FLAMES, 0);
                    break;
                    }
                break;
                }
                Location loc = event.getTo();
                switch (tpEffect) {
                case 0:
                    loc.getWorld().strikeLightningEffect(loc);
                break;
                default:
                    Location newLoc = new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                    newLoc.setY(newLoc.getY() + 1);
                    switch (tpEffect) {
                    case 1:
                        newLoc.getWorld().playEffect(newLoc, Effect.ENDER_SIGNAL, 0);
                    break;
                    case 2:
                        newLoc.getWorld().playEffect(newLoc, Effect.SMOKE, 0);
                    break;
                    case 3:
                        newLoc.getWorld().playEffect(newLoc, Effect.MOBSPAWNER_FLAMES, 0);
                    break;
                    }
                break;
                }
                player.sendMessage(ChatColor.GOLD + "WHOOSH!");
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onBlockBreak (BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (isJailed(player.getName())) {
            if (player.hasPermission("simplespawn.jail.immune")) {
                setJailed(player.getName(), false, null);
            } else {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You cannot break blocks while you're in jail!");
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onBlockPlace (BlockPlaceEvent event) {
        if (!event.isCancelled()) {
            Player player = event.getPlayer();
            if (isJailed(player.getName())) {
                if (player.hasPermission("simplespawn.jail.immune")) {
                    setJailed(player.getName(), false, null);
                } else {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "You cannot place blocks while you're in jail!");
                }
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onBlockIgnite (BlockIgniteEvent event) {
        if (!event.isCancelled() && event.getCause().equals(IgniteCause.FLINT_AND_STEEL)) {
            Player player = event.getPlayer();
            if (isJailed(player.getName())) {
                if (player.hasPermission("simplespawn.jail.immune")) {
                    setJailed(player.getName(), false, null);
                } else {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "You cannot place ignite blocks while you're in jail!");
                }
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onPlayerEmptyBucket (PlayerBucketEmptyEvent event) {
        if (!event.isCancelled()) {
            Player player = event.getPlayer();
            if (isJailed(player.getName())) {
                if (player.hasPermission("simplespawn.jail.immune")) {
                    setJailed(player.getName(), false, null);
                } else {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "You cannot empty your bucket while in jail!");
                }
            }
        }
    }
    
     @EventHandler (priority = EventPriority.NORMAL)
    public void onPlayerPVP (EntityDamageByEntityEvent event) {
        if (!event.isCancelled()) {
            Entity entity = event.getEntity();
            if (entity instanceof Player) {
                Player player = (Player)entity;
                if (isJailed(player.getName())) {
                    if (player.hasPermission("simplespawn.jail.immune")) {
                        setJailed(player.getName(), false, null);
                    } else {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "You cannot fight while in jail!");
                    }
                }
            }
        }
    }
}
