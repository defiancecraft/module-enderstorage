package com.defiancecraft.modules.enderstorage.commands;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;

import com.defiancecraft.core.command.ArgumentParser;
import com.defiancecraft.core.command.ArgumentParser.Argument;
import com.defiancecraft.modules.enderstorage.EnderStorage;
import com.defiancecraft.modules.enderstorage.banks.BankInventoryHolder;
import com.mongodb.MongoException;

public class BankCommands {

	@SuppressWarnings("deprecation")
	public static boolean enderChest(CommandSender sender, String[] args) {
		
		if (!(sender instanceof Player))
			return false;
		
		ArgumentParser parser = new ArgumentParser(String.join(" ", args), Argument.USERNAME);
		
		String name = parser.isValid() ? parser.getString(1) : sender.getName();
		
		// If they don't have perms to access others' ECs, deny
		if (!name.equalsIgnoreCase(sender.getName())
				&& !((Player)sender).hasPermission("defiancecraft.enderstorage.ec.others")) {
			return true;
		}
		
		// If they don't have perms for their own, send message, deny.
		if (name.equalsIgnoreCase(sender.getName())
				&& !((Player)sender).hasPermission("defiancecraft.enderstorage.ec")) {
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', EnderStorage.getConfiguration().shittyFuckingMessage));
			return true;
		}
		
		// World whitelist :S
		if (!EnderStorage.getConfiguration().isWorldWhitelisted(((Player)sender).getWorld().getName())) {
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', EnderStorage.getConfiguration().whitelistMsg));
			return true;
		}
		
		OfflinePlayer p;
		if (Bukkit.getPlayer(name) != null)
			p = Bukkit.getPlayer(name);
		else
			p = Bukkit.getOfflinePlayer(name);
		
		// Close & save if someone is viewing their bank.
		if (BankInventoryHolder.isUserViewingBank(p.getUniqueId())) {
			
			Player player = Bukkit.getPlayer(name);
			
			// Get their holder... safely.
			InventoryHolder holder = player == null ? null :
									 player.getOpenInventory() == null ? null :
								     player.getOpenInventory().getTopInventory() == null ? null :
								     player.getOpenInventory().getTopInventory().getHolder();	 
			
			if (Bukkit.getPlayer(name) == null 
					|| holder == null
					|| !(holder instanceof BankInventoryHolder)) {
				
				sender.sendMessage(ChatColor.RED + "Bank is saving. Please wait");
				return true;
				
			} else {
				
				sender.sendMessage(ChatColor.GRAY + "They are viewing their enderchest; please wait while I save it.");
				BankInventoryHolder bHolder = (BankInventoryHolder)holder;
				Future<?> future = bHolder.save();
				
				// Close their inventory (won't save twice)
				player.closeInventory();
				
				// Blocking call for saving.
				try {
					future.get();
				} catch (InterruptedException | ExecutionException e) {
					sender.sendMessage(ChatColor.RED + "Shit. Something went wrong saving their enderchest. Tell Dave to check the log, even though this totally isn't Dave's fault.");
					return true;
				}
				
				// Now we should be good.
				sender.sendMessage(ChatColor.GRAY + "Their enderchest was saved successfully!");
			}
			
		}
		
		try {
			if (p.getName().equals(sender.getName()))
				sender.sendMessage(ChatColor.GRAY + "Opening enderchest...");
			else
				sender.sendMessage(ChatColor.GRAY + "Opening their enderchest...");
			BankInventoryHolder holder = new BankInventoryHolder(p, (Player)sender);
			holder.open();
		} catch (IllegalStateException e) {
			sender.sendMessage(ChatColor.RED + "Player doesn't exist, or isn't saved in the database.");
		} catch (MongoException e) {
			sender.sendMessage(ChatColor.RED + "A database error occurred.");
			Bukkit.getLogger().severe("[ES01] MongoException when player attempted to open someone else's bank; ST below.");
			e.printStackTrace();
		}
		
		return true;
		
	}
	
}
