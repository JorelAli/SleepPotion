package io.github.Skepter.SleepPotion;

import io.github.Skepter.SleepPotion.PacketBuilder.PacketType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.Potion;

public class Main extends JavaPlugin implements Listener {

	/* Sets up the recipes and registers the events*/
	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		getCommand("sleeppotion").setExecutor(this);

		ShapedRecipe recipe1 = new ShapedRecipe(potionItemStack());
		recipe1.shape(new String[] { "WWW", "WPW", "WWW" });
		recipe1.setIngredient('W', Material.WOOL);
		recipe1.setIngredient('P', Material.POTION);
		getServer().addRecipe(recipe1);

		ShapedRecipe recipe2 = new ShapedRecipe(potionExtendedItemStack());
		recipe2.shape(new String[] { "WRW", "RPR", "WRW" });
		recipe2.setIngredient('W', Material.WOOL);
		recipe2.setIngredient('R', Material.REDSTONE);
		recipe2.setIngredient('P', Material.POTION);
		getServer().addRecipe(recipe2);
	}

	/* Wakes up all sleeping players in the event of a reload */
	@Override
	public void onDisable() {
		for (Player player : Bukkit.getOnlinePlayers())
			awakeFromBed(player);
	}

	private Player getPlayer(String player) {
		for (Player p : Bukkit.getOnlinePlayers())
			if (p.getName().equals(player))
				return p;
		return null;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!sender.isOp() || !sender.hasPermission("SleepPotion.command")) {
			sender.sendMessage("[SleepPotion] You do not have permissions to spawn a sleep potion");
			return true;
		}
		switch (args.length) {
		case 0:
		case 1:
			sender.sendMessage("[SleepPotion Help]");
			sender.sendMessage("The amount of potions to give is optional");
			sender.sendMessage("/sleeppotion give <Player> (amount)");
			sender.sendMessage("/sleeppotion giveextended <Player> (amount)");
		case 2:
			if (args[0].equalsIgnoreCase("give"))
					getPlayer(args[1]).getInventory().addItem(potionItemStack());
			else if (args[0].equalsIgnoreCase("giveextended"))
					getPlayer(args[1]).getInventory().addItem(potionExtendedItemStack());
			return true;
		case 3:
			if (args[0].equalsIgnoreCase("give"))
				for (int i = 0; i < Integer.parseInt(args[2]); i++)
					getPlayer(args[1]).getInventory().addItem(potionItemStack());
			else if (args[0].equalsIgnoreCase("giveextended"))
				for (int i = 0; i < Integer.parseInt(args[2]); i++)
					getPlayer(args[1]).getInventory().addItem(potionExtendedItemStack());
			return true;
		}
		return false;
	}

	/* Prevent players without permission from crafting */
	@EventHandler
	public void onCraft(PrepareItemCraftEvent event) {
		for (HumanEntity entity : event.getViewers()) {
			if (entity instanceof Player) {
				Player player = (Player) entity;
				if (event.getRecipe().getResult().isSimilar(potionItemStack())) {
					if (!player.hasPermission("SleepPotion.potion") || !player.isOp()) {
						event.getInventory().setItem(0, new ItemStack(Material.AIR));
						return;
					}
				} else if (event.getRecipe().getResult().isSimilar(potionExtendedItemStack())) {
					if (!player.hasPermission("SleepPotion.potionExtended") || !player.isOp()) {
						event.getInventory().setItem(0, new ItemStack(Material.AIR));
						return;
					}
				}
			}
		}
	}

	/* Puts players to sleep when the potion is thrown */
	@EventHandler
	public void onEvent(PotionSplashEvent event) {
		ItemStack is = event.getPotion().getItem();
		if (is.isSimilar(potionItemStack())) {
			for (Player player : getSplashedEntities(event.getAffectedEntities())) {
				event.setIntensity(player, 0.0D);
				doBedAction(player, false);
			}
		} else if (is.isSimilar(potionExtendedItemStack())) {
			for (Player player : getSplashedEntities(event.getAffectedEntities())) {
				event.setIntensity(player, 0.0D);
				doBedAction(player, true);
			}
		}
	}

	/* Makes a list of all of the players to affect */
	private List<Player> getSplashedEntities(Collection<LivingEntity> entities) {
		List<Player> players = new ArrayList<Player>();
		for (LivingEntity entity : entities)
			if (entity instanceof Player)
				players.add((Player) entity);
		return players;
	}

	/* The potion itemstack */
	private ItemStack potionItemStack() {
		ItemStack itemStack = new ItemStack(Material.POTION);
		Potion potion = new Potion(46);
		potion.setSplash(true);
		potion.apply(itemStack);
		ItemMeta meta = itemStack.getItemMeta();
		meta.setDisplayName("Potion of Sleeping");
		meta.setLore(Arrays.asList(new String[] { ChatColor.RED + "Sleepiness (0:05)" }));
		itemStack.setItemMeta(meta);
		itemStack.setDurability((short) 16384);
		return itemStack;
	}

	/* The extended potion itemstack*/
	private ItemStack potionExtendedItemStack() {
		ItemStack itemStack = new ItemStack(Material.POTION);
		Potion potion = new Potion(46);
		potion.setSplash(true);
		potion.apply(itemStack);
		ItemMeta meta = itemStack.getItemMeta();
		meta.setDisplayName("Potion of Sleeping");
		meta.setLore(Arrays.asList(new String[] { ChatColor.RED + "Sleepiness (0:10)" }));
		itemStack.setItemMeta(meta);
		itemStack.setDurability((short) 16384);
		return itemStack;
	}

	/* Puts the player to sleep, then wakes them up after a set time */
	private void doBedAction(final Player player, boolean extended) {
		putToBed(player);

		int duration = 5;
		if (extended)
			duration = 10;

		Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {

			@Override
			public void run() {
				awakeFromBed(player);
			}
		}, duration * 20);
	}

	/* Puts the player to sleep 
	 * a = the player to put to sleep
	 * b,c,d = the location of the player */
	private void putToBed(final Player player) {
		new PacketBuilder(player, PacketType.PLAY_OUT_BED).set("a", player.getEntityId()).setLocation("b", "c", "d", player.getLocation()).send();
	}

	/* Wakes the player up from sleep 
	 * a = the player to put to sleep
	 * b = the type of animation (2 being the wake up from sleep animation) */
	private void awakeFromBed(final Player player) {
		new PacketBuilder(player, PacketType.PLAY_OUT_ANIMATION).set("a", player.getEntityId()).setInt("b", 2).send();
	}
}
