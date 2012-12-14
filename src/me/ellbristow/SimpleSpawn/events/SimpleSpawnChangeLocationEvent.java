package me.ellbristow.SimpleSpawn.events;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;


public class SimpleSpawnChangeLocationEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private String locationName;
    private LocationType type;
    World world;
    double x, y, z;
    float yaw, pitch;
    
    public enum LocationType {
        WORLD_SPAWN,
        HOME,
        WORK,
        JAIL,
        RELEASE
    }
    
    public SimpleSpawnChangeLocationEvent(String locationName, LocationType locationType, Location loc) {
        this.locationName = locationName;
        this.type = locationType;
        this.world = loc.getWorld();
        this.x = loc.getX();
        this.y = loc.getY();
        this.z = loc.getZ();
        this.yaw = loc.getYaw();
        this.pitch = loc.getPitch();
    }
    
    public String getLocationName() {
        return locationName;
    }

    public float getPitch() {
        return pitch;
    }

    public LocationType getType() {
        return type;
    }

    public World getWorld() {
        return world;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public float getYaw() {
        return yaw;
    }

    public double getZ() {
        return z;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }

}
