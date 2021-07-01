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
	// the permission the user needs to be able to receive the clue (optional)
	private String permissionNeeded;
	// extra info about the clue once it is found. it is put on the lore
	private String extraInfo;
	// where the clue is located
	private Location location;
	
	public Clue(String title, String found, String reward, String permission,
				int x, int y, int z, String permissionNeeded, String extraInfo) {
		this.title = title;
		this.found = found;
		this.reward = reward;
		this.permission = permission;
		this.permissionNeeded = permissionNeeded;
		this.extraInfo = extraInfo;
		// assumes that the first world of the server is the main one, creates location
		this.location = new Location(ClueFinder.get().getServer().getWorlds().get(0), x, y, z);
	}
	
	public Clue(String title, String found, String reward, String permission,
				Location location, String permissionNeeded, String extraInfo) {
		this.title = title;
		this.found = found;
		this.reward = reward;
		this.permission = permission;
		this.permissionNeeded = permissionNeeded;
		this.extraInfo = extraInfo;
		this.location = location;
	}

	public String getTitle() {
		return title;
	}

	public String getFoundMessage() {
		return found;
	}

	public String getRewardMessage() {
		return reward;
	}

	public String getPermissionGiven() {
		return permission;
	}

	public String getPermissionNeeded() {
		return permissionNeeded;
	}

	public String getExtraInfo() {
		return extraInfo;
	}

	public Location getLocation() {
		return location;
	}

}
