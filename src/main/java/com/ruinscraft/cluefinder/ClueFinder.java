package com.ruinscraft.cluefinder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;

public class ClueFinder extends JavaPlugin implements Listener, CommandExecutor {

	private LuckPerms luckPerms;

	private static ClueFinder clueFinder;

	private String menuTitle;
	private String nearby;
	private String notAllowedTitle;
	private String viewClues;
	private List<Clue> clues;

	private Material foundItem;
	private Material nearItem;
	private Material unfoundItem;
	private Material notAllowedItem;

	// get an instance of the plugin
	public static ClueFinder get() {
		return clueFinder;
	}

	private Random random = new Random();

	@Override
	public void onEnable() {
		// sets the instance of the plugin to this instance
		clueFinder = this;

		// load LuckPerms
		RegisteredServiceProvider<LuckPerms> provider =
				Bukkit.getServicesManager().getRegistration(LuckPerms.class);
		if (provider != null) {
			getLogger().info("LuckPerms found!");
			luckPerms = provider.getProvider();
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
		this.notAllowedTitle = this.getConfig().getString("notAllowedTitle", "Find other clues before this one!");
		this.viewClues = this.getConfig().getString("viewClues", "See your clues: /clues");
		this.viewClues = ChatColor.translateAlternateColorCodes('&', this.viewClues);
		this.foundItem = Material.valueOf(this.getConfig().getString("foundItem", "GREEN_CONCRETE"));
		this.nearItem = Material.valueOf(this.getConfig().getString("nearItem", "LIME_CONCRETE"));
		this.unfoundItem = Material.valueOf(this.getConfig().getString("unfoundItem", "YELLOW_CONCRETE"));
		this.notAllowedItem = Material.valueOf(this.getConfig().getString("notAllowedItem", "BARRIER"));

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
						if (clue.getPermissionNeeded() == null ||
								(clue.getPermissionNeeded() != null && hasPermission(clue.getPermissionNeeded(), player))) {
							if (!hasPermission(clue.getPermissionGiven(), player)) {
								clueFound(player, clue);
							}
						}
					}
				}
			}
		}, 10, 20 * 5);

		// redstone colors for particles below
		Particle.DustOptions dustOptions = new Particle.DustOptions(org.bukkit.Color.fromRGB(128, 128, 128), 2);
		Particle.DustOptions notFoundClue = new Particle.DustOptions(org.bukkit.Color.fromRGB(250, 220, 0), 5);
		Particle.DustOptions foundClue = new Particle.DustOptions(org.bukkit.Color.fromRGB(40, 180, 60), 5);
		Particle.DustOptions foundAllClue = new Particle.DustOptions(org.bukkit.Color.fromRGB(254, 0, 0), 5);

		// cool particles around the clues
		this.getServer().getScheduler().runTaskTimer(this, () -> {
			List<Player> players = new ArrayList<>(this.getServer().getWorlds().get(0).getPlayers());
			double xDiff = (this.random.nextDouble() * 8) - 4;
			double yDiff = (this.random.nextDouble() * 5) - 2.5;
			double zDiff = (this.random.nextDouble() * 8) - 4;

			double xDiffCenter = this.random.nextDouble() - .5;
			double yDiffCenter = (this.random.nextDouble() * 6) - 3;
			double zDiffCenter = this.random.nextDouble() - .5;
			for (Player player : players) {
				for (Clue clue : this.clues) {
					Location clueLocation = clue.getLocation();
					if (player.getLocation().distanceSquared(clueLocation) > 900) { // around 30 block distance
						continue;
					}
					if (clue.getPermissionNeeded() == null || clue.getPermissionNeeded().equals("")
							|| hasPermission(clue.getPermissionNeeded(), player)
							|| hasPermission(clue.getPermissionGiven(), player)) {
						// if close enough to clue and hasn't found it yet, yay clue found
						player.spawnParticle(Particle.REDSTONE,
								clueLocation.clone().add(xDiff, yDiff, zDiff), 1, dustOptions);
						if (hasAllClues(player)) {
							player.spawnParticle(Particle.REDSTONE,
									clueLocation.clone().add(xDiffCenter, yDiffCenter, zDiffCenter), 1, foundAllClue);
						} else if (hasPermission(clue.getPermissionGiven(), player)) {
							player.spawnParticle(Particle.REDSTONE,
									clueLocation.clone().add(xDiffCenter, yDiffCenter, zDiffCenter), 1, foundClue);
						} else {
							player.spawnParticle(Particle.REDSTONE,
									clueLocation.clone().add(xDiffCenter, yDiffCenter, zDiffCenter), 1, notFoundClue);
						}
					}
				}
			}
		}, 10, 1);
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
			String permGiven = config.getString(whole + ".permission", "Permission");
			String permNeeded = config.getString(whole + ".needPermission", null);
			String extraInfo = config.getString(whole + ".foundDesc", null);
			int x = config.getInt(whole + ".x", 0);
			int y = config.getInt(whole + ".y", 64);
			int z = config.getInt(whole + ".z", 0);
			Clue clue = new Clue(title, found, reward, permGiven, x, y, z, permNeeded, extraInfo);

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
		player.sendMessage(clue.getFoundMessage());
		if (clue.getExtraInfo() != null) {
			player.sendMessage(ChatColor.GRAY + clue.getExtraInfo());
		}
		player.sendMessage(clue.getRewardMessage());
		// once the clue is found, the permission of the clue is set, and they can never find it again
		setPermission(clue.getPermissionGiven(), player);
		player.sendMessage(this.viewClues);

		if (hasAllClues(player) && !player.hasPermission("clues.tp")) {
			int rank = getConfig().getInt("numOfPeopleFinished", 0);
			rank++;
			getConfig().set("numOfPeopleFinished", rank);
			saveConfig();

			// this shouldnt be hard coded but i had to rush it also its broken
			player.sendMessage(ChatColor.YELLOW + "You found all of the clues!!!!!!");
			player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 1, 1);
			player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1);

			ItemStack customItem = new ItemStack(Material.NETHERITE_SWORD, 1);
			ItemMeta meta = customItem.getItemMeta();
			TextComponent textComponent = Component.text("MCATLAS 2 Year Anniversary Powder Hunt #" + rank)
					.color(TextColor.color(0x00FF00));
			meta.displayName(textComponent);

			String rankString = getSuffix(rank) + " player to find all the clues!";
			List<String> lore = new ArrayList<>();
			lore.add(rankString);
			meta.setLore(lore);

			meta.addEnchant(Enchantment.DAMAGE_ALL, 5, true);
			meta.addEnchant(Enchantment.FIRE_ASPECT, 3, true);
			meta.addEnchant(Enchantment.SWEEPING_EDGE, 3, true);

			customItem.setItemMeta(meta);

			customItem.addItemFlags(ItemFlag.HIDE_ENCHANTS);
			customItem.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

			if (!player.getInventory().addItem(customItem).isEmpty()) {
				player.getWorld().dropItemNaturally(player.getLocation(), customItem);
			}

			ItemStack goldBlock = new ItemStack(Material.GOLD_BLOCK, 64);
			player.getWorld().dropItemNaturally(player.getLocation(), goldBlock);

			ItemStack gold = new ItemStack(Material.GOLD_INGOT, 64);
			for (int i = 0; i < 20; i++) {
				player.getWorld().dropItemNaturally(player.getLocation(), gold);
			}

			player.getServer().broadcastMessage(ChatColor.YELLOW + player.getName() + " discovered all of the clues!!");
		}
	}

	public String getSuffix(int num) {
		int j = num % 10;
		int k = num % 100;
		if (j == 1 && k != 11) {
			return num + "st";
		}
		if (j == 2 && k != 12) {
			return num + "nd";
		}
		if (j == 3 && k != 13) {
			return num + "rd";
		}
		return num + "th";
	}

	// checks if someone has all clues
	public boolean hasAllClues(Player player) {
		for (Clue clue : getClues()) {
			if (!hasPermission(clue.getPermissionGiven(), player)) {
				return false;
			}
		}
		return true;
	}

	// if player explicitly has the permission, not if they are OP or have a * permission
	// example: powders.powder.Songs gives perm for all songs.
	// Player#hasPermission would return true for an individual song
	// this would not
	// necessary so sponsors can participate in powder hunt
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
		User user = luckPerms.getUserManager().getUser(player.getUniqueId());
		user.data().add(Node.builder(permission).build());
		luckPerms.getUserManager().saveUser(user);
		return true;
	}

	// command handler, runs whenever user types command
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) return false;
		Player player = (Player) sender;

		// open inventory with menuTitle
		if (args.length > 1) {
			if (args[0].equalsIgnoreCase("tp") && player.hasPermission("cluefinder.tp")) {
				int clueNum;
				try {
					clueNum = Integer.valueOf(args[1]);
				} catch (Exception e) { clueNum = 1; }
				Clue clue = getClues().get(clueNum - 1);
				player.teleport(clue.getLocation().clone().add(10, 0, 10));
				player.sendMessage("Teleported near clue " + clueNum + "!");
				return true;
			}
		}

		// if not a subcommand, then we're gonna open the clues menu

		Inventory inventory = Bukkit.createInventory(null, 45, this.menuTitle);
		// cycles through clues to create each item for the inventory
		int i = 0;
		for (Clue clue : getClues()) {
			i++;
			if (clue.getPermissionNeeded() != null && !hasPermission(clue.getPermissionNeeded(), player) && !hasPermission(clue.getPermissionGiven(), player)) {
				ItemStack clueItem = new ItemStack(this.notAllowedItem, 1);
				ItemMeta meta = clueItem.getItemMeta();
				TextComponent textComponent = Component.text(i + ": " + this.notAllowedTitle)
						.color(TextColor.color(0xFF0000));
				meta.displayName(textComponent);
				clueItem.setItemMeta(meta);
				inventory.addItem(clueItem);
				continue;
			}

			Material typeOfClue = null;

			if (hasPermission(clue.getPermissionGiven(), player)) {
				typeOfClue = this.foundItem;
			} else if (this.isNearClue(player.getLocation(), clue.getLocation())) {
				typeOfClue = this.nearItem;
			} else {
				typeOfClue = this.unfoundItem;
			}

			ItemStack clueItem = new ItemStack(typeOfClue, 1);

			ItemMeta meta = clueItem.getItemMeta();
			TextComponent titleText = Component.text(clue.getTitle());
			meta.displayName(titleText);
			if (typeOfClue == this.foundItem) {
				List<String> lore = new ArrayList<>();
				lore.add(clue.getFoundMessage());
				if (clue.getExtraInfo() != null) {
					// code from stackoverflow
					// splits lore into shorter parts, separated by space
					int maxLength = 50;
					Pattern p = Pattern.compile("\\G\\s*(.{1,"+maxLength+"})(?=\\s|$)", Pattern.DOTALL);
					Matcher m = p.matcher(clue.getExtraInfo());
					while (m.find()) {
						String lorePart = m.group(1);
						lore.add(lorePart);
					}
				}
				int maxLength = 50;
				Pattern p = Pattern.compile("\\G\\s*(.{1,"+maxLength+"})(?=\\s|$)", Pattern.DOTALL);
				Matcher m = p.matcher(clue.getRewardMessage());
				while (m.find()) {
					String lorePart = m.group(1);
					lore.add(lorePart);
				}
				meta.setLore(lore);
			} else if (typeOfClue == this.nearItem) {
				List<String> lore = new ArrayList<>();
				lore.add(this.nearby);
				meta.setLore(lore);
			}
			clueItem.setItemMeta(meta);

			inventory.addItem(clueItem);
		}

		// glass at the bottom
		ItemStack glass = new ItemStack(Material.WHITE_STAINED_GLASS_PANE, 1);
		ItemMeta glassItemMeta = glass.getItemMeta();
		TextComponent titleText = Component.text("");
		glassItemMeta.displayName(titleText);
		glass.setItemMeta(glassItemMeta);
		int[] slots = { 36, 37, 38, 39, 41, 42, 43, 44 };
		for (int slot : slots) {
			inventory.setItem(slot, glass);
		}

		// paper item in the last slot
		ItemStack paper = new ItemStack(Material.PAPER, 1);
		ItemMeta meta = paper.getItemMeta();
		meta.setDisplayName("What happens when you find all the clues?");
		paper.setItemMeta(meta);
		inventory.setItem(40, paper);

		player.openInventory(inventory);

		return true;
	}

	// prevents players from taking the items inside the inventory
	// also closes inventory if they click outside the main box
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInventoryClick(InventoryClickEvent event) {
		if (event.getInventory() == null) return;

		String title = event.getView().title().toString();
		title = title.substring(title.indexOf("content=") + 9, title.indexOf("\", "));

		if (title.equals(this.menuTitle)) {
			if (event.getClickedInventory() == null) {
				event.getWhoClicked().closeInventory();
			} else {
				event.setCancelled(true);
			}
		}
	}

}