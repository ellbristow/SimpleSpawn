package me.ellbristow.SimpleSpawn;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
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
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class SimpleSpawn extends JavaPlugin implements Listener {

    public static SimpleSpawn plugin;
    protected FileConfiguration config;
    private SQLBridge SSdb;
    private int tpEffect = 1;
    private int soundEffect = 1;
    private boolean useTpSound = true;
    private boolean useTpEffect = true;
    private boolean setHomeWithBeds = false;
    private boolean allowSpawnInJail = false;
    private Level logLevel = Level.INFO;
    private String[] spawnColumns = {"world", "x", "y", "z", "yaw", "pitch"};
    private String[] spawnDims = {"TEXT NOT NULL PRIMARY KEY", "DOUBLE NOT NULL DEFAULT 0", "DOUBLE NOT NULL DEFAULT 0", "DOUBLE NOT NULL DEFAULT 0", "FLOAT NOT NULL DEFAULT 0", "FLOAT NOT NULL DEFAULT 0"};
    private String[] backColumns = {"player", "world", "x", "y", "z", "yaw", "pitch"};
    private String[] backDims = {"TEXT NOT NULL PRIMARY KEY", "TEXT NOT NULL", "DOUBLE NOT NULL DEFAULT 0", "DOUBLE NOT NULL DEFAULT 0", "DOUBLE NOT NULL DEFAULT 0", "FLOAT NOT NULL DEFAULT 0", "FLOAT NOT NULL DEFAULT 0"};
    private String[] homeColumns = {"player", "world", "x", "y", "z", "yaw", "pitch"};
    private String[] homeDims = {"TEXT NOT NULL PRIMARY KEY", "TEXT NOT NULL", "DOUBLE NOT NULL DEFAULT 0", "DOUBLE NOT NULL DEFAULT 0", "DOUBLE NOT NULL DEFAULT 0", "FLOAT NOT NULL DEFAULT 0", "FLOAT NOT NULL DEFAULT 0"};
    private String[] workColumns = {"player", "world", "x", "y", "z", "yaw", "pitch"};
    private String[] workDims = {"TEXT NOT NULL PRIMARY KEY", "TEXT NOT NULL", "DOUBLE NOT NULL DEFAULT 0", "DOUBLE NOT NULL DEFAULT 0", "DOUBLE NOT NULL DEFAULT 0", "FLOAT NOT NULL DEFAULT 0", "FLOAT NOT NULL DEFAULT 0"};
    private String[] jailColumns = {"name", "world", "x", "y", "z", "yaw", "pitch"};
    private String[] jailDims = {"TEXT NOT NULL PRIMARY KEY", "TEXT NOT NULL", "DOUBLE NOT NULL DEFAULT 0", "DOUBLE NOT NULL DEFAULT 0", "DOUBLE NOT NULL DEFAULT 0", "FLOAT NOT NULL DEFAULT 0", "FLOAT NOT NULL DEFAULT 0"};
    private String[] releaseColumns = {"name", "world", "x", "y", "z", "yaw", "pitch"};
    private String[] releaseDims = {"TEXT NOT NULL PRIMARY KEY", "TEXT NOT NULL", "DOUBLE NOT NULL DEFAULT 0", "DOUBLE NOT NULL DEFAULT 0", "DOUBLE NOT NULL DEFAULT 0", "FLOAT NOT NULL DEFAULT 0", "FLOAT NOT NULL DEFAULT 0"};
    private String[] jailedColumns = {"player", "jailName"};
    private String[] jailedDims = {"TEXT NOT NULL PRIMARY KEY", "TEXT NOT NULL"};

    @Override
    public void onDisable() {
        SSdb.close();
    }

    @Override
    public void onEnable() {

        config = getConfig();
        try {
            logLevel = Level.parse(config.getString("loglevel").toUpperCase());
        } catch (IllegalArgumentException e) {
            getLogger().warning("loglevel [" + config.getString("loglevel") + "] in config.yml cannot be parsed.");
            logLevel = Level.INFO;
        } catch (NullPointerException e) {
            getLogger().warning("loglevel not found in config.yml cannot be parsed.");
            logLevel = Level.INFO;
        }
        getLogger().config("loglevel: " + logLevel.getName());
        getLogger().setLevel(logLevel);
        config.set("loglevel", logLevel.getName());

        useTpEffect = config.getBoolean("use_teleport_effect", true);
        getLogger().config("use_teleport_effect:" + useTpEffect);

        useTpSound = config.getBoolean("use_teleport_sound", true);
        getLogger().config("use_teleport_sound:" + useTpSound);

        config.set("use_teleport_effect", useTpEffect);
        config.set("use_teleport_sound", useTpSound);

        // Pessimism... a lot of people are not using 1.3.2 so just to be sure SOUND class exists.
        try {
            Class.forName("org.bukkit.Sound");
        } catch (ClassNotFoundException e) {
            getLogger().warning("Sound is disabled, because your bukkit version doesn't support it!");
            useTpSound = false;
        }

        tpEffect = config.getInt("teleport_effect_type", 1);
        getLogger().config("teleport_effect_type:" + tpEffect);
        config.set("teleport_effect_type", tpEffect);

        soundEffect = config.getInt("sound_effect_type", 1);
        getLogger().config("sound_effect_type:" + soundEffect);
        config.set("sound_effect_type", soundEffect);

        setHomeWithBeds = config.getBoolean("set_home_with_beds", true);
        getLogger().config("set_home_with_beds:" + setHomeWithBeds);
        config.set("set_home_with_beds", setHomeWithBeds);

        allowSpawnInJail = config.getBoolean("allow_spawn_in_jail", false);
        getLogger().config("allow_spawn_in_jail:" + allowSpawnInJail);
        config.set("allow_spawn_in_jail", allowSpawnInJail);

        saveConfig();
        SSdb = new SQLBridge(this);
        SSdb.getConnection();
        File ebean = new File(getDataFolder().getParentFile().getParentFile(), "ebean.properties");
        if (!ebean.exists()) {
            createEbean(ebean);
        }
        if (new File(getDataFolder(), "locations.yml").exists()) {
            convertDb();
        } else {
            if (!SSdb.checkTable("WorldSpawns")) {
                getLogger().info("Created table WorldSpawns in SimpleSpawn.db");
                SSdb.createTable("WorldSpawns", spawnColumns, spawnDims);
            }
            if (!SSdb.checkTable("BackSpawns")) {
                getLogger().info("Created table BackSpawns in SimpleSpawn.db");
                SSdb.createTable("BackSpawns", backColumns, backDims);
            }
            if (!SSdb.checkTable("PlayerHomes")) {
                getLogger().info("Created table PlayerHomes in SimpleSpawn.db");
                SSdb.createTable("PlayerHomes", homeColumns, homeDims);
            }
            if (!SSdb.checkTable("PlayerWorks")) {
                getLogger().info("Created table PlayerWorks in SimpleSpawn.db");
                SSdb.createTable("PlayerWorks", workColumns, workDims);
            }
            if (!SSdb.checkTable("DefaultSpawn")) {
                getLogger().info("Created table DefaultSpawn in SimpleSpawn.db");
                SSdb.createTable("DefaultSpawn", spawnColumns, spawnDims);
                setDefaultSpawn(getServer().getWorlds().get(0).getSpawnLocation());
            }
        }
        if (!SSdb.checkTable("Jails")) {
            getLogger().info("Created table Jails in SimpleSpawn.db");
            SSdb.createTable("Jails", jailColumns, jailDims);
        }
        if (!SSdb.checkTable("Releases")) {
            getLogger().info("Created table Releases in SimpleSpawn.db");
            SSdb.createTable("Releases", releaseColumns, releaseDims);
        }
        if (!SSdb.checkTable("Jailed")) {
            getLogger().info("Created table Jailed in SimpleSpawn.db");
            SSdb.createTable("Jailed", jailedColumns, jailedDims);
        }

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(this, this);
        getLogger().info("SimpleSpawn enabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (!(sender instanceof Player)) {
            getLogger().fine("Command " + commandLabel + " cannot be run from the console.");
            sender.sendMessage("Sorry! Command " + commandLabel + " cannot be run from the console!");
            return false;
        }

        Player player = (Player) sender;
        Boolean playerIsJailed = isJailed(player.getName());

        if (commandLabel.equalsIgnoreCase("setspawn")) {
            if (args.length == 0) {
                if (!player.hasPermission("simplespawn.set")) {
                    getLogger().fine("Player " + player.getName() + " has no simplespawn.set permission.");
                    player.sendMessage(ChatColor.RED + "You do not have permission to set the spawn location!");
                    return false;
                }
                if (playerIsJailed) {
                    getLogger().fine("Player " + player.getName() + " cannot set spawn location while in jail.");
                    player.sendMessage(ChatColor.RED + "You cannot set a spawn location while in jail!");
                    return false;
                }
                setWorldSpawn(player.getLocation());
                getLogger().finer("Player " + player.getName() + " set spawn location.");
                player.sendMessage(ChatColor.GOLD + "Spawn been set to this location for this world!");
                return true;
            } else if (args.length == 1) {
                if (args[0].equalsIgnoreCase("*default")) {
                    if (!player.hasPermission("simplespawn.set.default")) {
                        getLogger().fine("Player " + player.getName() + " has no simplespawn.set.default permission.");
                        player.sendMessage(ChatColor.RED + "You do not have permission to set the spawn location for new players!");
                        return false;
                    }
                    if (playerIsJailed) {
                        getLogger().fine("Player " + player.getName() + " cannot set a default spawn location while in jail.");
                        player.sendMessage(ChatColor.RED + "You cannot set a default spawn location while in jail!");
                        return false;
                    }
                    setDefaultSpawn(player.getLocation());
                    getLogger().finer("Player " + player.getName() + " sets a default spawn location.");
                    player.sendMessage(ChatColor.GOLD + "Spawn for new players been set to this location for new players!");
                    return true;
                } else {
                    getLogger().fine("Player " + player.getName() + " cannot set a default spawn location while in jail.");
                    player.sendMessage(ChatColor.RED + "Command not recognised!");
                    player.sendMessage(ChatColor.RED + "Try: /setspawn OR /setspawn *default");
                    return false;
                }
            } else {
                player.sendMessage(ChatColor.RED + "Command not recognised!");
                player.sendMessage(ChatColor.RED + "Try: /setspawn OR /setspawn *default");
                return false;
            }

        } else if (commandLabel.equalsIgnoreCase("spawn")) {
            if (args.length == 0) {
                if (!player.hasPermission("simplespawn.use")) {
                    getLogger().fine("Player " + player.getName() + " has no simplespawn.use permission.");
                    player.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
                    return false;
                }
                if (playerIsJailed && !allowSpawnInJail) {
                    getLogger().fine("Player " + player.getName() + " cannot spawn to a location while in jail.");
                    player.sendMessage(ChatColor.RED + "You cannot spawn while in jail!");
                    return false;
                }
                getLogger().finer("Player " + player.getName() + " spawns to spawn location in current world.");
                simpleTeleport(player, getWorldSpawn(player.getWorld().getName()));
                return true;
            } else if (args.length == 1) {
                if (args[0].equalsIgnoreCase("*default")) {
                    if (!player.hasPermission("simplespawn.use.default")) {
                        getLogger().fine("Player " + player.getName() + " has no simplespawn.use.default permission.");
                        player.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
                        return false;
                    }
                    if (playerIsJailed && !allowSpawnInJail) {
                        getLogger().fine("Player " + player.getName() + " cannot spawn to default location while in jail.");
                        player.sendMessage(ChatColor.RED + "You cannot spawn to default location while in jail!");
                        return false;
                    }
                    getLogger().finer("Player " + player.getName() + " spawns to default spawn location.");
                    simpleTeleport(player, getDefaultSpawn());
                    return true;
                }

                if (!player.hasPermission("simplespawn.use.world")) {
                    getLogger().fine("Player " + player.getName() + " has no simplespawn.use.world permission.");
                    player.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
                    return false;
                }
                if (playerIsJailed && !allowSpawnInJail) {
                    getLogger().fine("Player " + player.getName() + " cannot spawn to another world while in jail.");
                    player.sendMessage(ChatColor.RED + "You cannot spawn to another world while in jail!");
                    return false;
                }
                World world = getServer().getWorld(args[0]);
                if (world == null) {
                    getLogger().fine("Player " + player.getName() + " tries to spawn to unknown world " + args[0] + ".");
                    player.sendMessage(ChatColor.RED + "World '" + ChatColor.WHITE + args[0] + ChatColor.RED + "' not found!");
                    return false;
                }
                getLogger().finer("Player " + player.getName() + " spawns to world (" + world.getName() + ") spawn location.");
                simpleTeleport(player, getWorldSpawn(world.getName()));
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "Command not recognised!");
                player.sendMessage(ChatColor.RED + "Try: /spawn {worldName} or /spawn *default");
                return false;
            }

        } else if (commandLabel.equalsIgnoreCase("back")) {
            if (args.length == 0) {
                if (!player.hasPermission("simplespawn.back")) {
                    getLogger().fine("Player " + player.getName() + " has no simplespawn.back permission.");
                    player.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
                    return false;
                }
                if (playerIsJailed && !allowSpawnInJail) {
                    getLogger().fine("Player " + player.getName() + " cannot go back while in jail.");
                    player.sendMessage(ChatColor.RED + "You cannot go back while in jail!");
                    return false;
                }
                Location backLoc = getBackLoc(player.getName());
                getLogger().finer("Player " + player.getName() + " goes to previous location.");
                simpleTeleport(player, backLoc);
                return true;
            } else if (args.length == 1) {
                if (!player.hasPermission("simplespawn.home.use.others")) {
                    getLogger().fine("Player " + player.getName() + " has no simplespawn.home.use.others permission.");
                    player.sendMessage(ChatColor.RED + "You do not have permission to use that command to spawn to others home!");
                    return false;
                }
                if (playerIsJailed && !allowSpawnInJail) {
                    getLogger().fine("Player " + player.getName() + " cannot go to others home while in jail.");
                    player.sendMessage(ChatColor.RED + "You cannot go to others home while in jail!");
                    return false;
                }
                OfflinePlayer target = getServer().getOfflinePlayer(args[0]);
                if (!target.hasPlayedBefore() && !player.isOnline()) {
                    getLogger().fine("Player " + player.getName() + " tries to go to home location of unkown user " + target.getName() + ".");
                    player.sendMessage(ChatColor.RED + "Player '" + ChatColor.WHITE + args[0] + ChatColor.RED + "' not found!");
                    return false;
                }
                if (!target.isOnline() && !player.hasPermission("simplespawn.home.use.offline")) {
                    getLogger().fine("Player " + player.getName() + " has no simplespawn.home.use.offline permission.");
                    player.sendMessage(ChatColor.RED + "You do not have permission to use that command to spawn to offline players home!");
                    return false;
                }
                Location homeLoc = getHomeLoc(target.getName());
                if (homeLoc == null) {
                    getLogger().fine("Player " + player.getName() + " tries to go to home location of user " + target.getName() + " but that user has no home.");
                    player.sendMessage(ChatColor.RED + "Can't find " + ChatColor.WHITE + args[0] + ChatColor.RED + "'s home or bed!");
                    return false;
                }
                getLogger().finer("Player " + player.getName() + " goes to home location of " + target.getName() + ".");
                simpleTeleport(player, homeLoc);
                return true;
            }
        } else if (commandLabel.equalsIgnoreCase("sethome")) {
            if (args.length == 0) {
                if (!player.hasPermission("simplespawn.home.set")) {
                    getLogger().fine("Player " + player.getName() + " has no simplespawn.home.set permission.");
                    player.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
                    return false;
                }
                if (playerIsJailed) {
                    getLogger().fine("Player " + player.getName() + " cannot set a home location while in jail.");
                    player.sendMessage(ChatColor.RED + "You cannot set a home location while in jail!");
                    return false;
                }

                getLogger().finer("Player " + player.getName() + " sets his home location.");
                setHomeLoc(player);
                player.sendMessage(ChatColor.GOLD + "Your home has been set to this location!");
                return true;
            } else if (args.length == 1) {
                if (!player.hasPermission("simplespawn.home.set.others")) {
                    getLogger().fine("Player " + player.getName() + " has no simplespawn.home.set.others permission.");
                    player.sendMessage(ChatColor.RED + "You do not have permission to set other peoples home!");
                    return false;
                }
                if (playerIsJailed) {
                    getLogger().fine("Player " + player.getName() + " cannot set others home location while in jail.");
                    player.sendMessage(ChatColor.RED + "You cannot set others home location while in jail!");
                    return false;
                }

                OfflinePlayer target = getServer().getOfflinePlayer(args[0]);
                if (!target.hasPlayedBefore() && !player.isOnline()) {
                    getLogger().fine("Player " + player.getName() + " tries to set home location for unkown user " + target.getName() + ".");
                    player.sendMessage(ChatColor.RED + "Player '" + ChatColor.WHITE + args[0] + ChatColor.RED + "' not found!");
                    return true;
                }
                getLogger().finer("Player " + player.getName() + " sets the home location for " + target.getName() + ".");
                setOtherHomeLoc(target, player);
                player.sendMessage(ChatColor.WHITE + target.getName() + ChatColor.GOLD + "'s home has been set to this location!");
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "Command not recognised!");
                player.sendMessage(ChatColor.RED + "Try: /sethome OR /sethome {playerName}");
                return false;
            }

        } else if (commandLabel.equalsIgnoreCase("home")) {
            if (args.length == 0) {
                if (!player.hasPermission("simplespawn.home.use")) {
                    getLogger().fine("Player " + player.getName() + " has no simplespawn.home.use permission.");
                    player.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
                    return false;
                }
                if (playerIsJailed && !allowSpawnInJail) {
                    getLogger().fine("Player " + player.getName() + " cannot go home while in jail.");
                    player.sendMessage(ChatColor.RED + "You cannot go home while in jail!");
                    return false;
                }
                Location homeLoc = getHomeLoc(player);
                getLogger().finer("Player " + player.getName() + " goes to home location.");
                simpleTeleport(player, homeLoc);
                return true;
            } else if (args.length == 1) {
                if (!player.hasPermission("simplespawn.home.use.others")) {
                    getLogger().fine("Player " + player.getName() + " has no simplespawn.home.use.others permission.");
                    player.sendMessage(ChatColor.RED + "You do not have permission to use that command to spawn to others home!");
                    return false;
                }
                if (playerIsJailed && !allowSpawnInJail) {
                    getLogger().fine("Player " + player.getName() + " cannot go to others home while in jail.");
                    player.sendMessage(ChatColor.RED + "You cannot go to others home while in jail!");
                    return false;
                }
                OfflinePlayer target = getServer().getOfflinePlayer(args[0]);
                if (!target.hasPlayedBefore() && !player.isOnline()) {
                    getLogger().fine("Player " + player.getName() + " tries to go to home location of unkown user " + target.getName() + ".");
                    player.sendMessage(ChatColor.RED + "Player '" + ChatColor.WHITE + args[0] + ChatColor.RED + "' not found!");
                    return false;
                }
                if (!target.isOnline() && !player.hasPermission("simplespawn.home.use.offline")) {
                    getLogger().fine("Player " + player.getName() + " has no simplespawn.home.use.offline permission.");
                    player.sendMessage(ChatColor.RED + "You do not have permission to use that command to spawn to offline players home!");
                    return false;
                }
                Location homeLoc = getHomeLoc(target.getName());
                if (homeLoc == null) {
                    getLogger().fine("Player " + player.getName() + " tries to go to home location of user " + target.getName() + " but that user has no home.");
                    player.sendMessage(ChatColor.RED + "Can't find " + ChatColor.WHITE + args[0] + ChatColor.RED + "'s home or bed!");
                    return false;
                }
                getLogger().finer("Player " + player.getName() + " goes to home location of " + target.getName() + ".");
                simpleTeleport(player, homeLoc);
                return true;
            }
        } else if (commandLabel.equalsIgnoreCase("removehome")) {
            if (args.length == 0) {
                if (!player.hasPermission("simplespawn.home.remove")) {
                    getLogger().fine("Player " + player.getName() + " has no simplespawn.home.remove permission.");
                    player.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
                    return false;
                }
                if (playerIsJailed) {
                    getLogger().fine("Player " + player.getName() + " cannot remove a home location while in jail.");
                    player.sendMessage(ChatColor.RED + "You cannot remove a home location while in jail!");
                    return false;
                }
                getLogger().finer("Player " + player.getName() + " removes home location.");

                removeHome(player.getName());
                player.sendMessage(ChatColor.GOLD + "Your home location has been removed!");
                return true;
            } else if (args.length == 1) {
                if (!player.hasPermission("simplespawn.home.remove.others")) {
                    getLogger().fine("Player " + player.getName() + " has no simplespawn.home.remove.others permission.");
                    player.sendMessage(ChatColor.RED + "You do not have permission to remove other peoples work!");
                    return false;
                }
                if (playerIsJailed) {
                    getLogger().fine("Player " + player.getName() + " cannot remove a home location for others while in jail.");
                    player.sendMessage(ChatColor.RED + "You cannot remove a home location for others while in jail!");
                    return false;
                }
                OfflinePlayer target = getServer().getOfflinePlayer(args[0]);
                if (!target.hasPlayedBefore() && !player.isOnline()) {
                    getLogger().fine("Player " + player.getName() + " tries to remove home location of unkown user " + target.getName() + ".");
                    player.sendMessage(ChatColor.RED + "Player '" + ChatColor.WHITE + args[0] + ChatColor.RED + "' not found!");
                    return false;
                }
                getLogger().finer("Player " + player.getName() + " removes home location of " + target.getName() + ".");
                removeHome(target.getName());
                player.sendMessage(ChatColor.WHITE + target.getName() + ChatColor.GOLD + "'s home location has been removed!");
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "Command not recognised!");
                player.sendMessage(ChatColor.RED + "Try: /removehome OR /removehome {playerName}");
                return false;
            }

        } else if (commandLabel.equalsIgnoreCase("setwork")) {
            if (args.length == 0) {
                if (!player.hasPermission("simplespawn.work.set")) {
                    getLogger().fine("Player " + player.getName() + " has no simplespawn.work.set permission.");
                    player.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
                    return false;
                }
                if (playerIsJailed) {
                    getLogger().fine("Player " + player.getName() + " cannot set a work location while in jail.");
                    player.sendMessage(ChatColor.RED + "You cannot set a work location while in jail!");
                    return false;
                }
                getLogger().finer("Player " + player.getName() + " sets his work location.");
                setWorkLoc(player);
                player.sendMessage(ChatColor.GOLD + "Your work has been set to this location!");
                return true;
            } else if (args.length == 1) {
                if (!player.hasPermission("simplespawn.work.set.others")) {
                    getLogger().fine("Player " + player.getName() + " has no simplespawn.work.set.others permission.");
                    player.sendMessage(ChatColor.RED + "You do not have permission to set other peoples work!");
                    return false;
                }
                if (playerIsJailed) {
                    getLogger().fine("Player " + player.getName() + " cannot set a work location for others while in jail.");
                    player.sendMessage(ChatColor.RED + "You cannot set a work location for others while in jail!");
                    return false;
                }
                OfflinePlayer target = getServer().getOfflinePlayer(args[0]);
                if (!target.hasPlayedBefore() && !player.isOnline()) {
                    getLogger().fine("Player " + player.getName() + " tries to set work location for unkown user " + target.getName() + ".");
                    player.sendMessage(ChatColor.RED + "Player '" + ChatColor.WHITE + args[0] + ChatColor.RED + "' not found!");
                    return false;
                }
                getLogger().finer("Player " + player.getName() + " sets work location for " + target.getName() + ".");
                setOtherWorkLoc(target, player);
                player.sendMessage(ChatColor.WHITE + target.getName() + ChatColor.GOLD + "'s work has been set to this location!");
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "Command not recognised!");
                player.sendMessage(ChatColor.RED + "Try: /setwork OR /setwork {playerName}");
                return false;
            }

        } else if (commandLabel.equalsIgnoreCase("removework")) {
            if (args.length == 0) {
                if (!player.hasPermission("simplespawn.work.remove")) {
                    getLogger().fine("Player " + player.getName() + " has no simplespawn.work.remove permission.");
                    player.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
                    return false;
                }
                if (playerIsJailed) {
                    getLogger().fine("Player " + player.getName() + " cannot remove a work location while in jail.");
                    player.sendMessage(ChatColor.RED + "You cannot remove a work location while in jail!");
                    return false;
                }
                getLogger().finer("Player " + player.getName() + " removes his work location.");
                removeWork(player.getName());
                player.sendMessage(ChatColor.GOLD + "Your work location has been removed!");
                return true;
            } else if (args.length == 1) {
                if (!player.hasPermission("simplespawn.work.remove.others")) {
                    getLogger().fine("Player " + player.getName() + " has no simplespawn.work.remove.others permission.");
                    player.sendMessage(ChatColor.RED + "You do not have permission to remove other peoples work!");
                    return false;
                }
                if (playerIsJailed) {
                    getLogger().fine("Player " + player.getName() + " cannot remove a work location for others while in jail.");
                    player.sendMessage(ChatColor.RED + "You cannot remove a work location for others while in jail!");
                    return false;
                }
                OfflinePlayer target = getServer().getOfflinePlayer(args[0]);
                if (!target.hasPlayedBefore() && !player.isOnline()) {
                    getLogger().fine("Player " + player.getName() + " tries to remove work location for unkown user " + target.getName() + ".");
                    player.sendMessage(ChatColor.RED + "Player '" + ChatColor.WHITE + args[0] + ChatColor.RED + "' not found!");
                    return false;
                }
                getLogger().finer("Player " + player.getName() + " removes work location of " + target.getName() + ".");
                removeWork(target.getName());
                player.sendMessage(ChatColor.WHITE + target.getName() + ChatColor.GOLD + "'s work location has been removed!");
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "Command not recognised!");
                player.sendMessage(ChatColor.RED + "Try: /removework OR /removework {playerName}");
                return false;
            }

        } else if (commandLabel.equalsIgnoreCase("work")) {
            if (args.length == 0) {
                if (!player.hasPermission("simplespawn.work.use")) {
                    getLogger().fine("Player " + player.getName() + " has no simplespawn.work.use permission.");
                    player.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
                    return false;
                }
                if (playerIsJailed && !allowSpawnInJail) {
                    getLogger().fine("Player " + player.getName() + " cannot go to work while in jail.");
                    player.sendMessage(ChatColor.RED + "You cannot go to work while in jail!");
                    return false;
                }
                Location workLoc = getWorkLoc(player);
                if (workLoc == null) {
                    player.sendMessage(ChatColor.RED + "You haven't set your work!");
                    player.sendMessage(ChatColor.RED + "Try: /setwork OR /setwork {playerName}");
                    return false;
                }
                getLogger().finer("Player " + player.getName() + " goes to work location.");
                simpleTeleport(player, workLoc);
                return true;
            } else if (args.length == 1) {
                if (!player.hasPermission("simplespawn.work.use.others")) {
                    getLogger().fine("Player " + player.getName() + " has no simplespawn.work.use.others permission.");
                    player.sendMessage(ChatColor.RED + "You do not have permission to use that command to spawn to others work!");
                    return false;
                }
                if (playerIsJailed && !allowSpawnInJail) {
                    getLogger().fine("Player " + player.getName() + " cannot go to others work while in jail.");
                    player.sendMessage(ChatColor.RED + "You cannot go to others work while in jail!");
                    return false;
                }
                OfflinePlayer target = getServer().getOfflinePlayer(args[0]);
                if (!target.hasPlayedBefore() && !player.isOnline()) {
                    getLogger().fine("Player " + player.getName() + " tries to go to the work location of unkown user " + target.getName() + ".");
                    player.sendMessage(ChatColor.RED + "Player '" + ChatColor.WHITE + args[0] + ChatColor.RED + "' not found!");
                    return false;
                }
                if (!target.isOnline() && !player.hasPermission("simplespawn.work.use.offline")) {
                    getLogger().fine("Player " + player.getName() + " has no simplespawn.work.use.offline permission.");
                    player.sendMessage(ChatColor.RED + "You do not have permission to use that command to spawn to offline players work!");
                    return false;
                }
                Location workLoc = getWorkLoc(target.getName());
                if (workLoc == null) {
                    getLogger().fine("Player " + player.getName() + " tries to go to work location of user " + target.getName() + " but that user has no work.");
                    player.sendMessage(ChatColor.RED + "Player '" + ChatColor.WHITE + args[0] + ChatColor.RED + "' hasn't set a work location!");
                    player.sendMessage(ChatColor.RED + "Try: /setwork OR /setwork {playerName}");
                    return false;
                }
                getLogger().finer("Player " + player.getName() + " goes to work location of " + target.getName() + ".");
                simpleTeleport(player, workLoc);
                return true;
            }

        } else if (commandLabel.equalsIgnoreCase("setjail")) {
            if (!player.hasPermission("simplespawn.jail.set")) {
                getLogger().fine("Player " + player.getName() + " has no simplespawn.jail.set permission.");
                player.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
                return false;
            }
            if (playerIsJailed) {
                getLogger().fine("Player " + player.getName() + " cannot set a jail location while in jail.");
                player.sendMessage(ChatColor.RED + "You cannot set a jail location while in jail!");
                return false;
            }

            if (args.length == 0) {
                setJail("default", player.getLocation());
                getLogger().finer("Player " + player.getName() + " sets default jail.");
                player.sendMessage(ChatColor.GOLD + "Default jail set to your location!");
                return true;
            } else if (args.length == 1) {
                setJail(args[0], player.getLocation());
                getLogger().finer("Player " + player.getName() + " sets jail " + args[0] + ".");
                player.sendMessage(ChatColor.GOLD + "Jail '" + ChatColor.WHITE + args[0] + ChatColor.GOLD + "' set to your location!");
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "Command not recognised!");
                player.sendMessage(ChatColor.RED + "Try: /setjail OR /setjail {jailName}");
                return false;
            }

        } else if (commandLabel.equalsIgnoreCase("setrelease")) {
            if (!player.hasPermission("simplespawn.release.set")) {
                getLogger().fine("Player " + player.getName() + " has no simplespawn.release.set permission.");
                player.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
                return false;
            }
            if (playerIsJailed) {
                getLogger().fine("Player " + player.getName() + " cannot set a release location while in jail.");
                player.sendMessage(ChatColor.RED + "You cannot set a release location while in jail!");
                return false;
            }

            if (args.length == 0) {
                setRelease("default", player.getLocation());
                getLogger().finer("Player " + player.getName() + " sets default release.");
                player.sendMessage(ChatColor.GOLD + "Default release set to your location!");
                return true;
            } else if (args.length == 1) {
                setRelease(args[0], player.getLocation());
                getLogger().finer("Player " + player.getName() + " sets " + args[0] + " release.");
                player.sendMessage(ChatColor.GOLD + "Release '" + ChatColor.WHITE + args[0] + ChatColor.GOLD + "' set to your location!");
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "Command not recognised!");
                player.sendMessage(ChatColor.RED + "Try: /setrelease OR /setrelease {releaseName}");
                return false;
            }

        } else if (commandLabel.equalsIgnoreCase("spawnjail")) {
            if (!player.hasPermission("simplespawn.jail.spawn")) {
                getLogger().fine("Player " + player.getName() + " has no simplespawn.jail.spawn permission.");
                player.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
                return false;
            }
            String jailName = "default";
            if (args.length == 0) {
                if (playerIsJailed && !allowSpawnInJail) {
                    getLogger().fine("Player " + player.getName() + " cannot spawn to the default jail while in jail.");
                    player.sendMessage(ChatColor.RED + "You cannot spawn to the default jail while in jail!");
                    return false;
                }
            } else if (args.length == 1) {
                if (playerIsJailed && !allowSpawnInJail) {
                    getLogger().fine("Player " + player.getName() + " cannot spawn to another jail while in jail.");
                    player.sendMessage(ChatColor.RED + "You cannot spawn to another jail while in jail!");
                    return false;
                }

                jailName = args[0];
            }

            Location jailLocation = getJail(jailName);
            if (jailLocation == null) {
                getLogger().fine("Player " + player.getName() + " tries to spawns to jail " + jailName + " which is not found.");
                player.sendMessage(ChatColor.RED + "Jail '" + ChatColor.WHITE + args[0] + ChatColor.RED + "' not found!");
                return false;
            }
            getLogger().finer("Player " + player.getName() + " spawns to jail " + jailName + ".");
            simpleTeleport(player, jailLocation);
            return true;

        } else if (commandLabel.equalsIgnoreCase("spawnrelease")) {
            if (!player.hasPermission("simplespawn.release.spawn")) {
                getLogger().fine("Player " + player.getName() + " has no simplespawn.release.spawn permission.");
                player.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
                return false;
            }
            String releaseName = "default";
            if (args.length == 0) {
                if (playerIsJailed && !allowSpawnInJail) {
                    getLogger().fine("Player " + player.getName() + " cannot spawn to the default release location while in jail.");
                    player.sendMessage(ChatColor.RED + "You cannot spawn to the default release location while in jail!");
                    return false;
                }
            } else if (args.length == 1) {
                if (playerIsJailed && !allowSpawnInJail) {
                    getLogger().fine("Player " + player.getName() + " cannot spawn to a release location while in jail.");
                    player.sendMessage(ChatColor.RED + "You cannot spawn to a release location while in jail!");
                    return false;
                }

                releaseName = args[0];
            }

            Location releaseLocation = getRelease(releaseName);
            if (releaseLocation == null) {
                getLogger().fine("Player " + player.getName() + " tries to spawns to release " + releaseName + " which is not found.");
                player.sendMessage(ChatColor.RED + "Release '" + ChatColor.WHITE + args[0] + ChatColor.RED + "' not found!");
                return false;
            }
            getLogger().finer("Player " + player.getName() + " spawns to release " + releaseName + ".");
            simpleTeleport(player, releaseLocation);
            return true;

        } else if (commandLabel.equalsIgnoreCase("jail")) {
            if (!player.hasPermission("simplespawn.jail.use")) {
                getLogger().fine("Player " + player.getName() + " has no simplespawn.jail.use permission.");
                player.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
                return false;
            }
            if (playerIsJailed) {
                getLogger().fine("Player " + player.getName() + " cannot send someone to jail while in jail.");
                player.sendMessage(ChatColor.RED + "You cannot send someone to jail while in jail!");
                return false;
            }

            if (args.length == 1) {
                if (getJail("default") == null) {
                    getLogger().fine("Player " + player.getName() + " tries to send someone to the default jail which is not set.");
                    player.sendMessage(ChatColor.RED + "No default jail has been set!");
                    player.sendMessage(ChatColor.RED + "Use: /setjail");
                    return false;
                }
                if (args[0].equalsIgnoreCase(player.getName())) {
                    getLogger().fine("Player " + player.getName() + " can't jail self.");
                    player.sendMessage(ChatColor.RED + "You can't jail yourself!");
                    return false;
                }
                OfflinePlayer target = getServer().getOfflinePlayer(args[0]);
                if (!target.hasPlayedBefore() && !player.isOnline()) {
                    getLogger().fine("Player " + player.getName() + " tries to send unkown user " + args[0] + " to the default jail.");
                    player.sendMessage(ChatColor.RED + "Player '" + ChatColor.WHITE + args[0] + ChatColor.RED + "' not found!");
                    return false;
                }
                if (target.isOnline()) {
                    Player targetPlayer = target.getPlayer();
                    if (!targetPlayer.hasPermission("simplespawn.jail.immune")) {
                        getLogger().finer("Player " + player.getName() + " sends user " + targetPlayer.getName() + " to the default jail.");
                        simpleTeleport(targetPlayer, getJail("default"));
                        setJailed(target.getName(), true, "default");
                        getServer().broadcastMessage(target.getName() + ChatColor.GOLD + " has been jailed!");
                        return true;
                    } else {
                        getLogger().fine("Player " + player.getName() + " tries to send immune user " + targetPlayer.getName() + " to the default jail.");
                        player.sendMessage(targetPlayer.getName() + ChatColor.RED + " cannot be jailed!");
                        return false;
                    }
                } else {
                    getLogger().finer("Player " + player.getName() + " sentence user " + target.getName() + " to the default jail.");
                    setJailed(target.getName(), true, "default");
                    getServer().broadcastMessage(target.getName() + ChatColor.GOLD + " has been sentenced to jail!");
                }
                return true;
            } else if (args.length == 2) {
                if (getJail(args[1]) == null) {
                    getLogger().fine("Player " + player.getName() + " tries to send someone to the unknown jail " + args[1] + ".");
                    player.sendMessage(ChatColor.RED + "Could not find a jail called '" + ChatColor.WHITE + args[1] + ChatColor.RED + "'!");
                    player.sendMessage(ChatColor.RED + "Use: /jail [player] {jailName}");
                    return false;
                }
                if (args[0].equalsIgnoreCase(player.getName())) {
                    getLogger().fine("Player " + player.getName() + " can't jail self.");
                    player.sendMessage(ChatColor.RED + "You can't jail yourself!");
                    return false;
                }
                OfflinePlayer target = getServer().getOfflinePlayer(args[0]);
                if (!target.hasPlayedBefore() && !player.isOnline()) {
                    getLogger().fine("Player " + player.getName() + " tries to send unkown user " + args[0] + " to jail " + args[1] + ".");
                    player.sendMessage(ChatColor.RED + "Player '" + ChatColor.WHITE + args[0] + ChatColor.RED + "' not found!");
                    return false;
                }
                if (target.isOnline()) {
                    Player targetPlayer = target.getPlayer();
                    if (!targetPlayer.hasPermission("simplespawn.jail.immune")) {
                        getLogger().finer("Player " + player.getName() + " sends user " + targetPlayer.getName() + " to jail " + args[1] + ".");
                        simpleTeleport(targetPlayer, getJail(args[1]));
                        setJailed(target.getName(), true, args[1]);
                        getServer().broadcastMessage(target.getName() + ChatColor.GOLD + " has been jailed!");
                        return true;
                    } else {
                        getLogger().fine("Player " + player.getName() + " tries to send immune user " + targetPlayer.getName() + " to jail " + args[1] + ".");
                        player.sendMessage(targetPlayer.getName() + ChatColor.RED + " cannot be jailed!");
                        return true;
                    }
                } else {
                    getLogger().finer("Player " + player.getName() + " sentence user " + target.getName() + " to jail " + args[1] + ".");
                    setJailed(target.getName(), true, "default");
                    getServer().broadcastMessage(target.getName() + ChatColor.GOLD + " has been sentenced to jail!");
                    return true;
                }
            } else {
                player.sendMessage(ChatColor.RED + "Command not recognised!");
                player.sendMessage(ChatColor.RED + "Try: /jail [playerName] {jailName}");
                return false;
            }

        } else if (commandLabel.equalsIgnoreCase("release")) {
            if (!player.hasPermission("simplespawn.jail.release")) {
                getLogger().fine("Player " + player.getName() + " has no simplespawn.jail.release permission.");
                player.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
                return false;
            }
            if (playerIsJailed) {
                getLogger().fine("Player " + player.getName() + " cannot release a player while in jail.");
                player.sendMessage(ChatColor.RED + "You cannot release a player while in jail!");
                return false;
            }

            if (args.length == 1) {
                OfflinePlayer target = getServer().getOfflinePlayer(args[0]);
                if (!target.hasPlayedBefore() && !player.isOnline()) {
                    getLogger().fine("Player " + player.getName() + " tries to release unknown user " + args[0] + ".");
                    player.sendMessage(ChatColor.RED + "Player '" + ChatColor.WHITE + args[0] + ChatColor.RED + "' not found!");
                    return false;
                }
                if (!isJailed(target.getName())) {
                    getLogger().fine("Player " + player.getName() + " tries to release the free user " + args[0] + ".");
                    player.sendMessage(target.getName() + ChatColor.RED + " is not in jail!");
                    return false;
                }
                String currentJail = getWhereJailed(target.getName());
                setJailed(target.getName(), false, args[0]);
                getLogger().finer("Player " + player.getName() + " releases the user " + args[0] + ".");
                getServer().broadcastMessage(ChatColor.WHITE + target.getName() + ChatColor.GOLD + " has been released from jail!");
                Location releaseLoc = getRelease(currentJail);

                if (target.isOnline()) {
                    // if there is a release location for this jail... teleport Player there                    
                    if (releaseLoc != null) {
                        getLogger().finer("Player " + target.getName() + " is send to release " + currentJail + ".");
                        target.getPlayer().sendMessage(ChatColor.GOLD + "You are released from jail!");
                        simpleTeleport(target.getPlayer(), releaseLoc);
                        return true;
                    } else {
                        target.getPlayer().sendMessage(ChatColor.GOLD + "You may now leave the jail!");
                        return true;
                    }
                } else {
                    if (releaseLoc != null) {
                        player.sendMessage(ChatColor.WHITE + target.getName() + ChatColor.GOLD + " has been released from jail, but not send to releasePoint. [Player is offline]");
                        return true;
                    }
                }
            } else {
                player.sendMessage(ChatColor.RED + "Command not recognised!");
                player.sendMessage(ChatColor.RED + "Try: /release [playerName]");
                return false;
            }

        } else if (commandLabel.equalsIgnoreCase("removejail")) {
            if (!player.hasPermission("simplespawn.jail.remove")) {
                getLogger().fine("Player " + player.getName() + " has no simplespawn.jail.remove permission.");
                player.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
                return false;
            }
            if (playerIsJailed) {
                getLogger().fine("Player " + player.getName() + " cannot remove a jail while in jail.");
                player.sendMessage(ChatColor.RED + "You cannot remove a jail while in jail!");
                return false;
            }

            if (args.length == 0) {
                if (getJail("default") == null) {
                    getLogger().fine("Player " + player.getName() + " tries to remove not set default jail.");
                    player.sendMessage(ChatColor.RED + "No default jail has been set!");
                    player.sendMessage(ChatColor.RED + "Use: /setjail");
                    return false;
                }
                if (getRelease("default") != null) {
                    getLogger().fine("Player " + player.getName() + " tries to remove the default jail, but it has a release attached.");
                    player.sendMessage(ChatColor.RED + "There is still a default release location set!");
                    player.sendMessage(ChatColor.RED + "Use: /removerelease");
                    return false;
                }
                if (getInMates("default") != 0) {
                    getLogger().fine("Player " + player.getName() + " tries to remove the default jail, but it is not empty.");
                    player.sendMessage(ChatColor.RED + "Default jail is not empty!");
                    player.sendMessage(ChatColor.RED + "Use: /jails or /inmates");
                    return false;
                }

                getLogger().finer("Player " + player.getName() + " removes default jail.");
                removeJail("default");
                player.sendMessage(ChatColor.GOLD + "Default jail removed!");
                return true;

            } else if (args.length == 1) {
                if (getJail(args[0]) == null) {
                    getLogger().fine("Player " + player.getName() + " tries to remove unknown jail " + args[0] + ".");
                    player.sendMessage(ChatColor.RED + "Could not find a jail called '" + ChatColor.WHITE + args[0] + ChatColor.RED + "'!");
                    player.sendMessage(ChatColor.RED + "Use: /setjail {jailName}");
                    return false;
                }
                if (getRelease(args[0]) != null) {
                    getLogger().fine("Player " + player.getName() + " tries to remove jail " + args[0] + ", but that has a release attached.");
                    player.sendMessage(ChatColor.RED + "There is still a release location for " + ChatColor.WHITE + args[0] + ChatColor.RED + "set!");
                    player.sendMessage(ChatColor.RED + "Use: /removerelease {releaseName}");
                    return false;
                }
                if (getInMates(args[0]) == 0) {
                    getLogger().fine("Player " + player.getName() + " tries to remove jail " + args[0] + ", but that is not empty.");
                    player.sendMessage(ChatColor.RED + "Jail called " + ChatColor.WHITE + args[0] + ChatColor.RED + "is not empty!");
                    player.sendMessage(ChatColor.RED + "Use: /jails or /inmates");
                    return false;
                }
                getLogger().finer("Player " + player.getName() + " removes jail " + args[0] + ".");
                removeJail(args[0]);
                player.sendMessage(ChatColor.GOLD + "Jail called " + ChatColor.WHITE + args[0] + ChatColor.GOLD + " removed!");
                return true;
            }

        } else if (commandLabel.equalsIgnoreCase("removerelease")) {
            if (!player.hasPermission("simplespawn.release.remove")) {
                getLogger().fine("Player " + player.getName() + " has no simplespawn.release.remove permission.");
                player.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
                return false;
            }
            if (playerIsJailed) {
                getLogger().fine("Player " + player.getName() + " cannot remove a release location while in jail.");
                player.sendMessage(ChatColor.RED + "You cannot remove a release location while in jail!");
                return false;
            }

            if (args.length == 0) {
                if (getRelease("default") == null) {
                    getLogger().fine("Player " + player.getName() + " tries to remove not set default release.");
                    player.sendMessage(ChatColor.RED + "No default release has been set!");
                    player.sendMessage(ChatColor.RED + "Use: /setrelease");
                    return false;
                }
                getLogger().finer("Player " + player.getName() + " removes default release.");
                removeRelease("default");
                player.sendMessage(ChatColor.GOLD + "Default release removed!");
                return true;
            } else if (args.length == 1) {
                if (getRelease(args[0]) == null) {
                    getLogger().fine("Player " + player.getName() + " tries to remove unknown release " + args[0] + ".");
                    player.sendMessage(ChatColor.RED + "Could not find a release called '" + ChatColor.WHITE + args[0] + ChatColor.RED + "'!");
                    player.sendMessage(ChatColor.RED + "Use: /setrelease {releaseName}");
                    return false;
                }
                getLogger().finer("Player " + player.getName() + " removes release" + args[0] + ".");
                removeRelease(args[0]);
                player.sendMessage(ChatColor.GOLD + "Release called " + ChatColor.WHITE + args[0] + ChatColor.GOLD + " removed!");
                return true;
            }

        } else if (commandLabel.equalsIgnoreCase("inmates")) {
            if (!player.hasPermission("simplespawn.jail.inmates")) {
                getLogger().fine("Player " + player.getName() + " has no simplespawn.jail.inmates permission.");
                player.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
                return false;
            }

            String jailName = "default";
            if (args.length == 0) {
                if (getJail("default") == null) {
                    getLogger().fine("Player " + player.getName() + " tries to list inmates of default jail, which is not set.");
                    player.sendMessage(ChatColor.RED + "No default jail has been set!");
                    player.sendMessage(ChatColor.RED + "Use: /setjail");
                    return false;
                }
            } else if (args.length == 1) {
                if (getJail(args[0]) == null) {
                    getLogger().fine("Player " + player.getName() + " tries to list inmates of unkown jail " + args[0] + ".");
                    player.sendMessage(ChatColor.RED + "Could not find a jail called '" + ChatColor.WHITE + args[0] + ChatColor.RED + "'!");
                    player.sendMessage(ChatColor.RED + "Use: /setjail {jailName}");
                    return false;
                }
                jailName = args[0];
            }
            getLogger().finer("Player " + player.getName() + " list inmates of jail " + jailName + ".");
            player.sendMessage(ChatColor.GOLD + "Inmates in " + ChatColor.WHITE + jailName + " : " + ChatColor.GRAY + listInMates(jailName));
            return true;

        } else if (commandLabel.equalsIgnoreCase("jails")) {
            if (!player.hasPermission("simplespawn.jail.list")) {
                getLogger().fine("Player " + player.getName() + " has no simplespawn.jail.list permission.");
                player.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
                return false;
            }
            HashMap<Integer, HashMap<String, Object>> jailList = SSdb.select("name", "Jails", null, null, null);
            if (jailList == null || jailList.isEmpty()) {
                getLogger().fine("Player " + player.getName() + " tries to list jails, but none set.");
                player.sendMessage(ChatColor.RED + "No jails were found!");
                return true;
            }

            String jailMessage = ChatColor.GOLD + "Jails: ";
            for (int i = 0; i < jailList.size(); i++) {
                if (i != 0) {
                    jailMessage += ChatColor.GOLD + ", ";
                }
                String jailName = jailList.get(i).get("name").toString();
                jailMessage += ChatColor.WHITE + jailName;
                int inMates = getInMates(jailName);
                if (inMates != 0) {
                    jailMessage += ChatColor.GRAY + " (Inmates: " + inMates + ")";
                }
            }
            getLogger().finer("Player " + player.getName() + " lists all jails.");
            player.sendMessage(jailMessage);
            return true;
        }
        return false;
    }

    public void simpleTeleport(final Player player, final Location loc) {
        if (loc == null) {
            return;
        }

        Location fromLoc = player.getLocation();
        setBackLoc(player.getName(), fromLoc);

        playSound(fromLoc);
        playEffect(fromLoc);

        getServer().getScheduler().scheduleAsyncDelayedTask(this, new Runnable() {

            @Override
            public void run() {
                player.teleport(loc, TeleportCause.PLUGIN);
                getLogger().finer("Player " + player.getName() + " teleported");

                playSound(loc);
                playEffect(loc);
                player.sendMessage(ChatColor.GOLD + "WHOOSH!");
            }
        }, 10L);
    }

    public void setBackLoc(String playerName, Location loc) {
        String world = loc.getWorld().getName();
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        float yaw = loc.getYaw();
        float pitch = loc.getPitch();
        SSdb.query("INSERT OR REPLACE INTO BackSpawns (player, world, x, y, z, yaw, pitch) VALUES ('" + playerName + "', '" + world + "', " + x + ", " + y + ", " + z + ", " + yaw + ", " + pitch + ")");
    }

    public void setBedLoc(Player player) {
        Location homeLoc = player.getLocation();
        setHomeLoc(player);
        player.setBedSpawnLocation(homeLoc);
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
    }

    public Location getBackLoc(String playerName) {
        HashMap<Integer, HashMap<String, Object>> result = SSdb.select("world, x, y, z, yaw, pitch", "BackSpawns", "player = '" + playerName + "'", null, null);
        Location location;
        if (result == null || result.isEmpty()) {
            // if you haven't used /sethome - first home is your bed
            getLogger().finest("No previous location found for " + playerName + ", trying to retrieve bedspawn.");
            location = getServer().getOfflinePlayer(playerName).getBedSpawnLocation();
        } else {
            String world = (String) result.get(0).get("world");
            double x = (Double) result.get(0).get("x");
            double y = (Double) result.get(0).get("y");
            double z = (Double) result.get(0).get("z");
            float yaw = Float.parseFloat(result.get(0).get("yaw").toString());
            float pitch = Float.parseFloat(result.get(0).get("pitch").toString());
            location = new Location(getServer().getWorld(world), x, y, z, yaw, pitch);
        }
        return location;
    }

    public Location getHomeLoc(String playerName) {
        HashMap<Integer, HashMap<String, Object>> result = SSdb.select("world, x, y, z, yaw, pitch", "PlayerHomes", "player = '" + playerName + "'", null, null);
        Location location;
        if (result == null || result.isEmpty()) {
            // if you haven't used /sethome - first home is your bed
            getLogger().finest("No home found for " + playerName + ", trying to retrieve bedspawn.");
            location = getServer().getOfflinePlayer(playerName).getBedSpawnLocation();
        } else {
            String world = (String) result.get(0).get("world");
            double x = (Double) result.get(0).get("x");
            double y = (Double) result.get(0).get("y");
            double z = (Double) result.get(0).get("z");
            float yaw = Float.parseFloat(result.get(0).get("yaw").toString());
            float pitch = Float.parseFloat(result.get(0).get("pitch").toString());
            location = new Location(getServer().getWorld(world), x, y, z, yaw, pitch);
        }
        return location;
    }

    public Location getHomeLoc(Player player) {
        Location homeLoc = getHomeLoc(player.getName());

        // Default Spawn Location in this world if no Home/Bed set
        if (homeLoc == null) {
            getLogger().finest("No home/bed found for " + player.getName() + ", trying to retrieve spawn location.");
            homeLoc = getWorldSpawn(player.getWorld().getName());
        }

        return homeLoc;
    }

    public void setWorkLoc(Player player) {
        Location homeLoc = player.getLocation();
        String world = homeLoc.getWorld().getName();
        double x = homeLoc.getX();
        double y = homeLoc.getY();
        double z = homeLoc.getZ();
        float yaw = homeLoc.getYaw();
        float pitch = homeLoc.getPitch();
        SSdb.query("INSERT OR REPLACE INTO PlayerWorks (player, world, x, y, z, yaw, pitch) VALUES ('" + player.getName() + "', '" + world + "', " + x + ", " + y + ", " + z + ", " + yaw + ", " + pitch + ")");
    }

    public void setOtherWorkLoc(OfflinePlayer target, Player player) {
        Location homeLoc = player.getLocation();
        String world = homeLoc.getWorld().getName();
        double x = homeLoc.getX();
        double y = homeLoc.getY();
        double z = homeLoc.getZ();
        float yaw = homeLoc.getYaw();
        float pitch = homeLoc.getPitch();
        SSdb.query("INSERT OR REPLACE INTO PlayerWorks (player, world, x, y, z, yaw, pitch) VALUES ('" + target.getName() + "', '" + world + "', " + x + ", " + y + ", " + z + ", " + yaw + ", " + pitch + ")");
    }

    public Location getWorkLoc(Player player) {
        return getWorkLoc(player.getName());
    }

    public Location getWorkLoc(String playerName) {
        HashMap<Integer, HashMap<String, Object>> result = SSdb.select("world, x, y, z, yaw, pitch", "PlayerWorks", "player = '" + playerName + "'", null, null);
        Location location;
        if (result == null || result.isEmpty()) {
            getLogger().finest("No work found for " + playerName + ".");
            return null;
        } else {
            String world = (String) result.get(0).get("world");
            double x = (Double) result.get(0).get("x");
            double y = (Double) result.get(0).get("y");
            double z = (Double) result.get(0).get("z");
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
        if (result == null || result.isEmpty()) {
            getLogger().finest("No jail " + jailName + " found.");
            return null;
        } else {
            String world = (String) result.get(0).get("world");
            double x = (Double) result.get(0).get("x");
            double y = (Double) result.get(0).get("y");
            double z = (Double) result.get(0).get("z");
            float yaw = Float.parseFloat(result.get(0).get("yaw").toString());
            float pitch = Float.parseFloat(result.get(0).get("pitch").toString());
            location = new Location(getServer().getWorld(world), x, y, z, yaw, pitch);
        }
        return location;
    }

    public void setRelease(String releaseName, Location loc) {
        releaseName = releaseName.toLowerCase();
        String world = loc.getWorld().getName();
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        float yaw = loc.getYaw();
        float pitch = loc.getPitch();
        SSdb.query("INSERT OR REPLACE INTO Releases (name, world, x, y, z, yaw, pitch) VALUES ('" + releaseName + "', '" + world + "', " + x + ", " + y + ", " + z + ", " + yaw + ", " + pitch + ")");
    }

    public Location getRelease(String releaseName) {
        releaseName = releaseName.toLowerCase();
        HashMap<Integer, HashMap<String, Object>> result = SSdb.select("world, x, y, z, yaw, pitch", "Releases", "name = '" + releaseName + "'", null, null);
        Location location;
        if (result == null || result.isEmpty()) {
            getLogger().finest("No release " + releaseName + " found.");
            return null;
        } else {
            String world = (String) result.get(0).get("world");
            double x = (Double) result.get(0).get("x");
            double y = (Double) result.get(0).get("y");
            double z = (Double) result.get(0).get("z");
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

        return (String) jailed.get(0).get("jailName");
    }

    public int getInMates(String jailName) {
        HashMap<Integer, HashMap<String, Object>> inMates = SSdb.select("player", "Jailed", "jailName = '" + jailName + "'", null, null);
        if (inMates == null || inMates.isEmpty()) {
            return 0;
        }

        return inMates.size();
    }

    public String listInMates(String jailName) {
        String matesList = "";
        HashMap<Integer, HashMap<String, Object>> inMates = SSdb.select("player", "Jailed", "jailName = '" + jailName + "'", null, null);
        if (inMates != null && !inMates.isEmpty()) {
            for (int i = 0; i < inMates.size(); i++) {
                if (i != 0) {
                    matesList += ",";
                }
                matesList += inMates.get(i).get("player");
            }
        }

        return matesList;
    }

    public void removeHome(String playerName) {
        SSdb.query("DELETE FROM PlayerHomes WHERE player='" + playerName + "' ");
    }

    public void removeWork(String playerName) {
        SSdb.query("DELETE FROM PlayerWorks WHERE player='" + playerName + "' ");
    }

    public void removeJail(String jailName) {
        SSdb.query("DELETE FROM Jails WHERE name='" + jailName + "' ");
    }

    public void removeRelease(String releaseName) {
        SSdb.query("DELETE FROM Releases WHERE name='" + releaseName + "' ");
    }

    // ONLY FOR NEW PLAYERS
    public void setDefaultSpawn(Location location) {
        String world = location.getWorld().getName();
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        float yaw = location.getYaw();
        float pitch = location.getPitch();
        SSdb.query("INSERT OR REPLACE INTO DefaultSpawn (world, x, y, z, yaw, pitch) VALUES ('" + world + "', " + x + ", " + y + ", " + z + ", " + yaw + ", " + pitch + ")");
        location.getWorld().setSpawnLocation((int) x, (int) y, (int) z);
    }

    // ONLY FOR NEW PLAYERS
    public Location getDefaultSpawn() {
        HashMap<Integer, HashMap<String, Object>> result = SSdb.select("world, x, y, z, yaw, pitch", "DefaultSpawn", null, null, null);
        Location location;
        if (result == null || result.isEmpty()) {
            getLogger().finest("No default spawn found using spawn of first world (" + getServer().getWorlds().get(0).getName() + ").");
            location = getServer().getWorlds().get(0).getSpawnLocation();
        } else {
            String world = (String) result.get(0).get("world");
            double x = (Double) result.get(0).get("x");
            double y = (Double) result.get(0).get("y");
            double z = (Double) result.get(0).get("z");
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
        location.getWorld().setSpawnLocation((int) x, (int) y, (int) z);
    }

    public Location getWorldSpawn(String world) {
        HashMap<Integer, HashMap<String, Object>> result = SSdb.select("x, y, z, yaw, pitch", "WorldSpawns", "world = '" + world + "'", null, null);
        Location location;
        if (result == null || result.isEmpty()) {
            getLogger().finest("No world spawn found for world (" + world + ") in db, using minecraft spawn location.");
            location = getServer().getWorld(world).getSpawnLocation();
        } else {
            double x = (Double) result.get(0).get("x");
            double y = (Double) result.get(0).get("y");
            double z = (Double) result.get(0).get("z");
            float yaw = Float.parseFloat(result.get(0).get("yaw").toString());
            float pitch = Float.parseFloat(result.get(0).get("pitch").toString());
            location = new Location(getServer().getWorld(world), x, y, z, yaw, pitch);
        }
        return location;
    }

    private void convertDb() {
        FileConfiguration usersConfig;
        File usersFile;
        int count = 0;
        usersFile = new File(getDataFolder(), "locations.yml");
        usersConfig = YamlConfiguration.loadConfiguration(usersFile);

        getLogger().info("starting converting locations.yml to SQLite SimpleSpawn.db.");

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
                count++;
            }
        }
        getLogger().info("Converted " + count + " world locations.");

        Location defaultLoc = getServer().getWorlds().get(0).getSpawnLocation();
        SSdb.createTable("DefaultSpawn", spawnColumns, spawnDims);
        setDefaultSpawn(defaultLoc);

        SSdb.createTable("PlayerHomes", homeColumns, homeDims);
        OfflinePlayer[] players = getServer().getOfflinePlayers();
        count = 0;
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
                count++;
            }
        }
        getLogger().info("Converted " + count + " home locations.");

        usersFile.delete();
        getLogger().info("locations.yml converted.");
    }

    private void createEbean(File ebean) {
        try {
            Writer output = new BufferedWriter(new FileWriter(ebean));
            output.write("ebean.search.jars=bukkit.jar");
            output.close();
            getLogger().info("ebean.properties created!");
        } catch (IOException e) {
            String message = "Error Creating ebean.properties: " + e.getMessage();
            getLogger().severe(message);
        }
    }

    /*
     * EVENT LISTENERS
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        if (!player.hasPlayedBefore() && !player.isOnline()) {
            getLogger().fine("Player " + player.getName() + " has never played before and was send to default spawn.");
            getServer().getScheduler().scheduleAsyncDelayedTask(this, new Runnable() {

                @Override
                public void run() {
                    simpleTeleport(player, getDefaultSpawn());
                }
            });
            return;
        }
        removeImmuneFromJail(player);

        if (isJailed(player.getName())) {
            getLogger().fine("Player " + player.getName() + " was send to jail during join.");
            getServer().getScheduler().scheduleAsyncDelayedTask(this, new Runnable() {

                @Override
                public void run() {
                    simpleTeleport(player, getJail(getWhereJailed(player.getName())));
                }
            });
            getServer().broadcastMessage(player.getName() + ChatColor.GOLD + " has been jailed!");
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location respawn = null;
        removeImmuneFromJail(player);

        if (isJailed(player.getName())) {
            respawn = getJail(getWhereJailed(player.getName()));
            getLogger().fine("Player " + player.getName() + " was send to jail.");
            getServer().broadcastMessage(player.getName() + ChatColor.GOLD + " has been jailed for respawn!");
        } else {
            if (event.isBedSpawn() && !setHomeWithBeds) {
                getLogger().fine("Player " + player.getName() + " get bed spawn location for respawn.");
                respawn = player.getBedSpawnLocation();
            } else {
                getLogger().fine("Player " + player.getName() + " get home location for respawn.");
                respawn = getHomeLoc(player);
            }
        }

        if (respawn == null) {
            getLogger().fine("Player " + player.getName() + " no location for respawn found, using world spawn location for respawn.");
            respawn = player.getWorld().getSpawnLocation();
        }
        event.setRespawnLocation(respawn);
    }

    public void removeImmuneFromJail(Player player) {
        if (player.hasPermission("simplespawn.jail.immune")) {
            if (isJailed(player.getName())) {
                setJailed(player.getName(), false, null);
                getLogger().fine("Player " + player.getName() + " was pardoned from serving jail.");
                getServer().broadcastMessage(player.getName() + ChatColor.GOLD + " was pardoned from serving jail time!");
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.isCancelled()) {
            Player player = event.getPlayer();

            if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && event.getClickedBlock().getType().equals(Material.BED_BLOCK)) {
                if (setHomeWithBeds) {
                    if (isJailed(player.getName())) {
                        getLogger().fine("Player " + player.getName() + " cannot set a home location while in jail.");
                        player.sendMessage(ChatColor.RED + "You cannot set a home location while in jail!");
                    } else {
                        setBedLoc(player);
                        getLogger().finer("Player " + player.getName() + " your home is set to bed location.");
                        player.sendMessage(ChatColor.GOLD + "Your home has been set to this bed location!");
                    }
                }
            } else {
                if (isJailed(player.getName())) {
                    event.setCancelled(true);
                    if (!event.getAction().equals(Action.PHYSICAL)) {
                        getLogger().fine("Player " + player.getName() + " cannot interact with the world while in jail.");
                        player.sendMessage(ChatColor.RED + "You cannot interact with the world while in jail!");
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player player = event.getEntity();
        Location loc = player.getLocation();

        setBackLoc(player.getName(), loc);

    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!event.isCancelled()) {
            if (event.getCause().equals(TeleportCause.COMMAND)) {
                Player player = event.getPlayer();

                if (isJailed(player.getName())) {
                    event.setTo(getJail(getWhereJailed(player.getName())));
                    getLogger().fine("Player " + player.getName() + " cannot teleport while in jail.");
                    player.sendMessage(ChatColor.RED + "You cannot teleport while you're in jail!");
                    event.setCancelled(true);
                    return;
                }

                Location fromLoc = event.getFrom();

                setBackLoc(player.getName(), fromLoc);

                playSound(fromLoc);
                playEffect(fromLoc);

                getLogger().finer("Player " + player.getName() + " teleported.");
                Location toLoc = event.getTo();

                playSound(toLoc);
                playEffect(toLoc);
                player.sendMessage(ChatColor.GOLD + "WHOOSH!");
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!event.isCancelled()) {
            Player player = event.getPlayer();

            if (isJailed(player.getName())) {
                event.setCancelled(true);
                getLogger().fine("Player " + player.getName() + " cannot break blocks while in jail.");
                player.sendMessage(ChatColor.RED + "You cannot break blocks while you're in jail!");
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!event.isCancelled()) {
            Player player = event.getPlayer();

            if (isJailed(player.getName())) {
                event.setCancelled(true);
                getLogger().fine("Player " + player.getName() + " cannot place blocks while in jail.");
                player.sendMessage(ChatColor.RED + "You cannot place blocks while you're in jail!");
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (!event.isCancelled() && event.getCause().equals(IgniteCause.FLINT_AND_STEEL)) {
            Player player = event.getPlayer();

            if (isJailed(player.getName())) {
                event.setCancelled(true);
                getLogger().fine("Player " + player.getName() + " cannot ignite blocks while in jail.");
                player.sendMessage(ChatColor.RED + "You cannot ignite blocks while you're in jail!");
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerEmptyBucket(PlayerBucketEmptyEvent event) {
        if (!event.isCancelled()) {
            Player player = event.getPlayer();

            if (isJailed(player.getName())) {
                event.setCancelled(true);
                getLogger().fine("Player " + player.getName() + " cannot empty bucket while in jail.");
                player.sendMessage(ChatColor.RED + "You cannot empty your bucket while in jail!");
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerPVP(EntityDamageByEntityEvent event) {
        if (!event.isCancelled()) {
            // Are you getting damaged?
            Entity entity = event.getEntity();
            if (entity instanceof Player) {
                Player player = (Player) entity;

                if (isJailed(player.getName())) {
                    event.setCancelled(true);
                    getLogger().fine("Player " + player.getName() + " cannot be damaged while in jail.");
                    player.sendMessage(ChatColor.RED + "You cannot be damaged while in jail!");
                    // send notify to damager
                    entity = event.getDamager();
                    if (entity instanceof Player) {
                        player = (Player) entity;
                        getLogger().fine("Player " + player.getName() + " cannot fight while in jail.");
                        player.sendMessage(ChatColor.RED + "You cannot fight with someone in jail!");
                    }
                }
            }

            // Are you damaging others
            entity = event.getDamager();
            if (entity instanceof Player) {
                Player player = (Player) entity;

                if (isJailed(player.getName())) {
                    event.setCancelled(true);
                    getLogger().fine("Player " + player.getName() + " cannot fight while in jail.");
                    player.sendMessage(ChatColor.RED + "You cannot fight while in jail!");
                }
            }
        }
    }

    public void playSound(Location loc) {
        if (useTpSound) {
            switch (soundEffect) {
                case 0:
                    loc.getWorld().playSound(loc, Sound.AMBIENCE_THUNDER, 1, 1);
                    break;
                case 1:
                    loc.getWorld().playSound(loc, Sound.ENDERMAN_TELEPORT, 1, 1);
                    break;
                case 2:
                    loc.getWorld().playSound(loc, Sound.FIRE, 1, 1);
                    break;
                case 3:
                    loc.getWorld().playSound(loc, Sound.EXPLODE, 1, 1);
                    break;
                case 4:
                    loc.getWorld().playSound(loc, Sound.FIZZ, 1, 1);
                    break;
                case 5:
                    loc.getWorld().playSound(loc, Sound.PORTAL_TRIGGER, 1, 1);
                    break;
            }
        }
    }

    public void playEffect(Location loc) {
        if (useTpEffect) {
            switch (tpEffect) {
                case 0:
                    loc.getWorld().strikeLightningEffect(loc);
                    break;
                default:
                    loc.setY(loc.getY() + 1);
                    switch (tpEffect) {
                        case 1:
                            loc.getWorld().playEffect(loc, Effect.ENDER_SIGNAL, 0);
                            break;
                        case 2:
                            loc.getWorld().playEffect(loc, Effect.SMOKE, 0);
                            break;
                        case 3:
                            loc.getWorld().playEffect(loc, Effect.MOBSPAWNER_FLAMES, 0);
                            break;
                    }
                    break;
            }
        }
    }
}
