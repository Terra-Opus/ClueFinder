package com.ruinscraft.cluefinder;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.User;
import net.md_5.bungee.api.ChatColor;

public class ClueFinder extends JavaPlugin implements Listener, CommandExecutor {

	private LuckPermsApi luckPermsApi;

	private static ClueFinder clueFinder;

	private String menuTitle;
	private String nearby;
	private String viewClues;
	private List<Clue> clues;

	private Material foundItem;
	private Material nearItem;
	private Material unfoundItem;

	// get an instance of the plugin
	public static ClueFinder get() {
		return clueFinder;
	}

	@Override
	public void onEnable() {
		// sets the instance of the plugin to this instance
		clueFinder = this;

		// load LuckPerms
		RegisteredServiceProvider<LuckPermsApi> provider = 
				Bukkit.getServicesManager().getRegistration(LuckPermsApi.class);
		if (provider != null) {
			getLogger().info("LuckPerms found!");
			luckPermsApi = provider.getProvider();
		} else {
			getLogger().info("LuckPerms not found! Disabling.");
			Bukkit.getPluginManager().disablePlugin(this);
		}

		// creates the list of Clues to be used
		this.clues = new ArrayList<>();

		// registers command and events to watch for within this class
		getCommand("clues").setExecutor(this);
		this.getServer().getPluginManager().registerEvents(this, this);

		// loads the default config if a config isn't already there
		this.saveDefaultConfig();

		// sets some values from the config
		this.menuTitle = this.getConfig().getString("menuTitle", "Menu");
		this.nearby = this.getConfig().getString("nearby", "Near");
		this.viewClues = this.getConfig().getString("viewClues", "See your clues: /clues");
		this.viewClues = ChatColor.translateAlternateColorCodes('&', this.viewClues);
		this.foundItem = Material.valueOf(this.getConfig().getString("foundItem", "GREEN_CONCRETE"));
		this.nearItem = Material.valueOf(this.getConfig().getString("nearItem", "LIME_CONCRETE"));
		this.unfoundItem = Material.valueOf(this.getConfig().getString("unfoundItem", "YELLOW_CONCRETE"));

		// loads clues
		loadClues();

		// runs a timer every 6 seconds to go through every player and check if they are near to a clue
		this.getServer().getScheduler().runTaskTimer(this, () -> {
			List<Player> players = new ArrayList<>(this.getServer().getWorlds().get(0).getPlayers());
			for (Player player : players) {
				Location location = player.getLocation();
				for (Clue clue : this.clues) {
					// if close enough to clue and hasn't found it yet, yay clue found
					if (clue.getLocation().distanceSquared(location) < 64) {
						if (!hasPermission(clue.getPermission(), player)) {
							clueFound(player, clue);
						}
					}
				}
			}

		}, 0, 120); // (20 * 6)
	}

	@Override
	public void onDisable() {
		clueFinder = null;
	}

	// lore added to an item to say that a clue is nearby
	public String getNearbyString() {
		return nearby;
	}

	// title of the /clues menu
	public String getMenuTitle() {
		return menuTitle;
	}

	// get all clues
	public List<Clue> getClues() {
		return clues;
	}

	// loads all of the clues as Clue objects on init, and adds them to the clues list
	public void loadClues() {
		FileConfiguration config = this.getConfig();
		for (String section : config.getConfigurationSection("clues").getKeys(false)) {
			String whole = "clues." + section;

			String title = config.getString(whole + ".title", "Title");
			title = ChatColor.translateAlternateColorCodes('&', title);
			String found = config.getString(whole + ".found", "Found");
			found = ChatColor.translateAlternateColorCodes('&', found);
			String reward = config.getString(whole + ".reward", "Reward");
			reward = ChatColor.translateAlternateColorCodes('&', reward);
			String permission = config.getString(whole + ".permission", "Permission");
			int x = config.getInt(whole + ".x", 0);
			int y = config.getInt(whole + ".y", 64);
			int z = config.getInt(whole + ".z", 0);
			Clue clue = new Clue(title, found, reward, permission, x, y, z);

			this.clues.add(clue);
		}
	}

	// within 8 blocks
	public boolean isAtClue(Location player, Location clue) {
		if (player.getWorld() != this.getServer().getWorlds().get(0)) return false;
		if (player.distanceSquared(clue) <= 64) return true;
		return false;
	}

	// within 250 blocks
	public boolean isNearClue(Location player, Location clue) {
		if (player.getWorld() != this.getServer().getWorlds().get(0)) return false;
		if (player.distanceSquared(clue) <= 62500) return true;
		return false;
	}

	// run if someone finds a clue
	public void clueFound(Player player, Clue clue) {
		player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
		player.sendMessage(clue.getFound());
		player.sendMessage(clue.getReward());
		// once the clue is found, the permission of the clue is set, and they can never find it again
		setPermission(clue.getPermission(), player);
		player.sendMessage(this.viewClues);

		if (hasAllClues(player)) {
			// this shouldnt be hard coded but i had to rush it also its broken
			player.sendMessage(ChatColor.GOLD + "AHHHHH!!!!!!");
			player.sendMessage(ChatColor.YELLOW + "You found all of the clues!");
			player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 1, 1);
			ItemStack shulker = new ItemStack(Material.RED_SHULKER_BOX, 1);

			player.getInventory().addItem(shulker);
			ItemStack gold = new ItemStack(Material.GOLD_INGOT, 64);
			for (int i = 0; i < 36; i++) {
				player.getInventory().addItem(gold);
			}
			setPermission("powder.powder.rainbowcolumn", player);
			player.sendMessage(ChatColor.LIGHT_PURPLE + "You've been given the RainbowColumn Powder! /powder RainbowColumn");
		}
	}

	// checks if someone has all clues
	public boolean hasAllClues(Player player) {
		for (Clue clue : getClues()) {
			if (!hasPermission(clue.getPermission(), player)) {
				return false;
			}
		}
		return true;
	}

	// if player explicitly has the permission, not if they are OP or whatever
	public boolean hasPermission(String permission, Player player) {
		for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
			if (permission.equals(info.getPermission())) {
				return true;
			}
		}

		return false;
	}

	// sets permission for user using luckperms api
	public boolean setPermission(String permission, Player player) {
		User user = luckPermsApi.getUser(player.getName());
		Node node = luckPermsApi.buildNode(permission).setValue(true).build();
		if (user.setPermission(node).asBoolean()) {
			luckPermsApi.getUserManager().saveUser(user);
			return true;
		}
		return false;
	}

	// command handler, runs whenever user types command
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) return false;
		Player player = (Player) sender;

		// open inventory with menuTitle
		if (args.length > 0) {
			if (args[0].equalsIgnoreCase("tp") && player.hasPermission("cluefinder.tp")) {
				int clueNum;
				try {
					clueNum = Integer.valueOf(args[1]);
				} catch (Exception e) { clueNum = 1; }
				Clue clue = getClues().get(clueNum - 1);
				player.teleport(clue.getLocation());
				player.sendMessage("Teleported to clue " + clueNum + "!");
				return true;
			}
		}

		// if not a subcommand, then we're gonna open the clues menu

		Inventory inventory = Bukkit.createInventory(null, 36, this.menuTitle);
		// cycles through clues to create each item for the inventory
		for (Clue clue : getClues()) {
			Material typeOfClue = null;
			if (hasPermission(clue.getPermission(), player)) {
				typeOfClue = this.foundItem;
			} else if (this.isNearClue(player.getLocation(), clue.getLocation())) {
				typeOfClue = this.nearItem;
			} else {
				typeOfClue = this.unfoundItem;
			}

			ItemStack clueItem = new ItemStack(typeOfClue, 1);

			ItemMeta meta = clueItem.getItemMeta();
			meta.setDisplayName(clue.getTitle());
			if (typeOfClue == this.foundItem) {
				List<String> lore = new ArrayList<>();
				lore.add(clue.getFound());
				lore.add(clue.getReward());
				meta.setLore(lore);
			} else if (typeOfClue == this.nearItem) {
				List<String> lore = new ArrayList<>();
				lore.add(this.nearby);
				meta.setLore(lore);
			}
			clueItem.setItemMeta(meta);

			inventory.addItem(clueItem);
		}

		// paper item in the last slot
		ItemStack paper = new ItemStack(Material.PAPER, 1);
		ItemMeta meta = paper.getItemMeta();
		meta.setDisplayName("What happens when you find all of them?");
		paper.setItemMeta(meta);
		inventory.setItem(35, paper);

		player.openInventory(inventory);

		return true;
	}

	// prevents players from taking the items inside the inventory
	// also closes inventory if they click outside the main box
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInventoryClick(InventoryClickEvent event) {
		if (event.getInventory() == null) return;
		if (event.getInventory().getTitle().equals(this.menuTitle)) {
			if (event.getClickedInventory() == null) {
				event.getWhoClicked().closeInventory();
			} else {
				event.setCancelled(true);
			}
		}
	}

}