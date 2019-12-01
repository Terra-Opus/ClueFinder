package com.ruinscraft.cluefinder;

import org.bukkit.Location;

public class Clue {

	// title of the item in the menu
	private String title;
	// message sent when clue is found
	private String found;
	// message sent of the reward
	private String reward;
	// the permission given to user when they find the clue
	private String permission;
	// where the clue is located
	private Location location;
	
	public Clue(String title, String found, String reward, String permission, int x, int y, int z) {
		this.title = title;
		this.found = found;
		this.reward = reward;
		this.permission = permission;
		// assumes that the first world of the server is the main one, creates location
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
