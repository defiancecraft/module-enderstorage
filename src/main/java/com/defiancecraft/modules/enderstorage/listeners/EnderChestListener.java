package com.defiancecraft.modules.enderstorage.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.defiancecraft.modules.enderstorage.banks.BankInventoryHolder;

public class EnderChestListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerInteract(PlayerInteractEvent e) {
		
		// Return if not ender chest, not right
		// click, not allowed, or cancelled.
		if (e.isCancelled()
				|| !e.getAction().equals(Action.RIGHT_CLICK_BLOCK)
				|| !e.getClickedBlock().getType().equals(Material.ENDER_CHEST)
				|| e.useInteractedBlock().equals(Result.DENY))
			return;

		// Cancel, so other plugins don't take it.
		e.setUseInteractedBlock(Result.ALLOW);
		e.setCancelled(true);
		
		Player p = e.getPlayer();
		if (BankInventoryHolder.isUserViewingBank(p.getUniqueId())) {
			p.sendMessage(ChatColor.GRAY + "Please wait...");
			return;
		}
		
		BankInventoryHolder holder = new BankInventoryHolder(p);
		holder.open();
		
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onItemDragged(InventoryDragEvent e) {
		
		// We only care about BankInventoryHolders
		if (!(e.getInventory().getHolder() instanceof BankInventoryHolder))
			return;
		
		// Stop item drags across dummy items
		BankInventoryHolder bank = (BankInventoryHolder) e.getInventory().getHolder();
		e.getNewItems().forEach((slot, stack) -> {
			if (bank.isBankSlot(slot) && !bank.isAllowedSlot(slot))
				e.getNewItems().remove(slot);
		});
		
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onItemClick(InventoryClickEvent e) {
		
		// We only care about BankInventoryHolders
		if (!(e.getInventory().getHolder() instanceof BankInventoryHolder))
			return;
		
		// Prevent clicking if it's a dummy item
		BankInventoryHolder bank = (BankInventoryHolder) e.getInventory().getHolder();
		if (bank.isBankSlot(e.getRawSlot())
				&& !bank.isAllowedSlot(e.getRawSlot())) {
			e.setCancelled(true);
			e.setResult(Result.DENY);
			return;
		}
		
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onInventoryClose(InventoryCloseEvent e) {
		
		// We only care about BankInventoryHolders
		if (!(e.getInventory().getHolder() instanceof BankInventoryHolder))
		    return;
		
		// Save the bank on close, if not already saving.
		BankInventoryHolder bank = (BankInventoryHolder) e.getInventory().getHolder();
		if (!bank.isSaving())
			bank.save();
		
	}
	
}