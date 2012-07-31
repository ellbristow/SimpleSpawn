
0.2.5
  Commands	/setrelease {jailName} /removerelease
  Permissions simplespawn.release.set simplespawn.release.remove
  Sets spawn location for players that are released from a jail

  Commands  /setwork {player} /work {player}
  Permissions simplespawn.work.set simplespawn.work.set.other
  simplespawn.work.use simplespawn.work.use.other
  Set a work location
  
  Command	/inmates {jailName}
  Permissions simplespawn.jail.inmates
  Lists all playernames that are in that jail
  
  Command	/removejail {jailName}
  Permissions simplespawn.jail.remove
  Remove an empty jail (with no releaselocation)
  
  Commandenhancement
  Permissions simplespawn.home.use.other
  Teleport to other's home location

  Configuration
  allow_spawn_in_jail	Ability to use /spawn/work/home/... while in Jail

  Bugfix
  Can't use beds to set home location while in jail.
  (also can't do sethome/setwork/home/work... while in jail)
  
  Fixed /spawn to react on current/given world.
  added /spawndefault to spawn to "new players" spawnpoint, reusing simplespawn.use.default
  Added simplespawn.set.default permission 
  
0.2.4 Initial starting point