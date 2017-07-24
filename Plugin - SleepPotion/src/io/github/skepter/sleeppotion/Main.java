package io.github.skepter.sleeppotion;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;

public class Main extends JavaPlugin implements Listener {

	/* Sets up the recipes and registers the events*/
	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		getCommand("sleeppotion").setExecutor(this);
		saveDefaultConfig();

		ShapedRecipe recipe1 = new ShapedRecipe(new NamespacedKey(this, "sleeppotion"), potionItemStack());
		recipe1.shape(new String[] { "WWW", "WPW", "WWW" });
		recipe1.setIngredient('W', Material.WOOL);
		recipe1.setIngredient('P', Material.POTION);
		getServer().addRecipe(recipe1);

		ShapedRecipe recipe2 = new ShapedRecipe(new NamespacedKey(this, "sleeppotionextended"), potionExtendedItemStack());
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
		if(command.getName().equalsIgnoreCase("sleeppotion")) {
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
				break;
			case 2:
				if (args[0].equalsIgnoreCase("give"))
						getPlayer(args[1]).getInventory().addItem(potionItemStack());
				else if (args[0].equalsIgnoreCase("giveextended"))
						getPlayer(args[1]).getInventory().addItem(potionExtendedItemStack());
				break;
			case 3:
				if (args[0].equalsIgnoreCase("give"))
					for (int i = 0; i < Integer.parseInt(args[2]); i++)
						getPlayer(args[1]).getInventory().addItem(potionItemStack());
				else if (args[0].equalsIgnoreCase("giveextended"))
					for (int i = 0; i < Integer.parseInt(args[2]); i++)
						getPlayer(args[1]).getInventory().addItem(potionExtendedItemStack());
				break;
			}
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
			for (Player player : getSplashedPlayers(event.getAffectedEntities())) {
				event.setIntensity(player, 0.0D);
				doBedAction(player, false);
			}
		} else if (is.isSimilar(potionExtendedItemStack())) {
			for (Player player : getSplashedPlayers(event.getAffectedEntities())) {
				event.setIntensity(player, 0.0D);
				doBedAction(player, true);
			}
		}
	}

	/* Makes a list of all of the players to affect */
	private List<Player> getSplashedPlayers(Collection<LivingEntity> entities) {
		List<Player> players = new ArrayList<Player>();
		for (LivingEntity entity : entities)
			if (entity instanceof Player)
				players.add((Player) entity);
		return players;
	}

	/* The potion itemstack */
	private ItemStack potionItemStack() {
		ItemStack itemStack = new ItemStack(Material.SPLASH_POTION);
		PotionMeta meta = (PotionMeta) itemStack.getItemMeta();
		meta.setBasePotionData(new PotionData(PotionType.AWKWARD));
		meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
		meta.setDisplayName("Potion of Sleeping");
		meta.setLore(Arrays.asList(new String[] { ChatColor.RED + "Sleepiness " + getDurationString(false) }));
		itemStack.setItemMeta(meta);
//		itemStack.setDurability((short) 16384);
		return itemStack;
	}

	/* The extended potion itemstack*/
	private ItemStack potionExtendedItemStack() {
		ItemStack itemStack = new ItemStack(Material.SPLASH_POTION);
		PotionMeta meta = (PotionMeta) itemStack.getItemMeta();
		meta.setBasePotionData(new PotionData(PotionType.AWKWARD));
		meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
		meta.setDisplayName("Potion of Sleeping");
		meta.setLore(Arrays.asList(new String[] { ChatColor.RED + "Sleepiness " + getDurationString(true) }));
		itemStack.setItemMeta(meta);
//		itemStack.setDurability((short) 16384);
		return itemStack;
	}
	
	private String getDurationString(boolean extended) {
		int duration = getConfig().getInt("potionTime");
		if (extended)
			duration = getConfig().getInt("extendedPotionTime");
		
		int mins = duration / 60;
		int seconds = duration % 60;
		
		if(seconds < 10) {
			return "(" + mins + ":0" + seconds + ")"; 
		} else {
			return "(" + mins + ":" + seconds + ")"; 
		}
	}

	/* Puts the player to sleep, then wakes them up after a set time */
	private void doBedAction(final Player player, boolean extended) {
		
		putToBed(player);

		int duration = getConfig().getInt("potionTime");
		if (extended)
			duration = getConfig().getInt("extendedPotionTime");

		Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {

			@Override
			public void run() {
				awakeFromBed(player);
			}
		}, duration * 20);
	}
	
	@SuppressWarnings("deprecation")
	private void putToBed(final Player player) {
		player.sendBlockChange(player.getLocation(), Material.BED_BLOCK, (byte) 0);
		
		PacketContainer packet = new PacketContainer(PacketType.Play.Server.BED);
		packet.getIntegers().write(0, player.getEntityId());
		packet.getBlockPositionModifier().write(0, new BlockPosition(player.getLocation().toVector()));
		try {
			ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("deprecation")
	private void awakeFromBed(final Player player) {
		player.sendBlockChange(player.getLocation(), player.getLocation().getBlock().getType(), (byte) 0);
		
		PacketContainer packet = new PacketContainer(PacketType.Play.Server.ANIMATION);
		packet.getIntegers().write(0, player.getEntityId());
		packet.getIntegers().write(1, 2);
		try {
			ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
}
