package com.defiancecraft.modules.enderstorage.banks;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;

import com.defiancecraft.modules.enderstorage.EnderStorage;

/**
 * Utility class containing Bank related methods.
 */
public class BankUtils {

	/**
	 * Gets the number of rows a player is allowed, based
	 * on their permissions.
	 * 
	 * @param p Player to get number of rows for
	 * @return Number of allowed rows (will default to config's default)
	 */
	public static int getAllowedRows(Player p) {
		
		int rows = -1;
		
		for (PermissionAttachmentInfo info : p.getEffectivePermissions()) {
			if (info.getPermission().matches("^enderstorage\\.rows\\.\\d+$")) {
				int newRows = Integer.parseInt(info.getPermission().substring(info.getPermission().lastIndexOf('.') + 1));
				if (newRows > rows)
					rows = newRows;
			}
		}
		
		return rows == -1 ? EnderStorage.getConfiguration().defaultRows :
			   rows;
		
	}
	
	/**
	 * Compacts an inventory by stacking items
	 * 
	 * @param inventory Inventory to stack
	 * @param invSize Size of inventory
	 */
	@SuppressWarnings("deprecation")
	public static void compactInventory(ItemStack[] inventory, int invSize) {
		
		nextItem:
		for (int i = 0; i < invSize; i++) {
			
			ItemStack a = inventory[i];
			if (a == null || a.getAmount() >= a.getMaxStackSize()) continue;
			
			// Go through all the other items, and try to
			// stack them with this.
			for (int j = i + 1; j < invSize; j++) {
				
				ItemStack b = inventory[j];
				if (b == null) continue;
				
				if (b.getType().equals(a.getType())
						&& b.getDurability() == a.getDurability()
						&& b.getData().getData() == a.getData().getData()
						&& b.getAmount() < b.getMaxStackSize()
						&& ((b.getItemMeta() == null && a.getItemMeta() == null)
								|| (b.getItemMeta() != null && b.getItemMeta().equals(a.getItemMeta())))) {
					
					// If it will fit into b
					if (b.getAmount() + a.getAmount() <= b.getMaxStackSize()) {
						b.setAmount(b.getAmount() + a.getAmount());
						inventory[i] = null;
						continue nextItem;
						
					// Won't fit into b.
					} else {
						// Amount needed for full stack
						int needed = b.getMaxStackSize() - b.getAmount();
						b.setAmount(b.getAmount() + needed);
						a.setAmount(a.getAmount() - needed);
					}
				}
				
			}
			
		}
		
	}
	
}
