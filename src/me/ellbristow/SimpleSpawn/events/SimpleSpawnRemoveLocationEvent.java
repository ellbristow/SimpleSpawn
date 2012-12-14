package me.ellbristow.SimpleSpawn.events;

import me.ellbristow.SimpleSpawn.events.SimpleSpawnChangeLocationEvent.LocationType;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;


public class SimpleSpawnRemoveLocationEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private String locationName;
    private LocationType type;
    
    public SimpleSpawnRemoveLocationEvent(String locationName, LocationType locationType) {
        this.locationName = locationName;
        this.type = locationType;
    }
    
    public String getLocationName() {
        return locationName;
    }

    public LocationType getType() {
        return type;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }

}
