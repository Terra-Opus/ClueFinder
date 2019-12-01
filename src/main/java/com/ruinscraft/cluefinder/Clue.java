package com.ruinscraft.cluefinder;

import org.bukkit.Location;

public class Clue {

	private String title;
	private String found;
	private String reward;
	private String permission;
	private Location location;
	
	public Clue(String title, String found, String reward, String permission, int x, int y, int z) {
		this.title = title;
		this.found = found;
		this.reward = reward;
		this.permission = permission;
		this.location = new Location(ClueFinder.get().getServer().getWorlds().get(0), x, y, z);
	}
	
	public Clue(String title, String found, String reward, String permission, Location location) {
		this.title = title;
		this.found = found;
		this.reward = reward;
		this.permission = permission;
		this.location = location;
	}

	public String getTitle() {
		return title;
	}

	public String getFound() {
		return found;
	}

	public String getReward() {
		return reward;
	}

	public String getPermission() {
		return permission;
	}

	public Location getLocation() {
		return location;
	}

}
