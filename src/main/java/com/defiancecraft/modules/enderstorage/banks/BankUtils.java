package com.defiancecraft.modules.enderstorage.banks;

import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;

import com.defiancecraft.core.DefianceCore;
import com.defiancecraft.core.api.User;
import com.defiancecraft.core.database.documents.DBUser;
import com.defiancecraft.core.permissions.PermissionConfig;
import com.defiancecraft.core.permissions.PermissionConfig.Group;
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
	 * Gets the number of rows a player is allowed, loading
	 * their permissions from the database (and possibly their
	 * UUID, if the OfflinePlayer did not contain it)
	 * 
	 * @param p OfflinePlayer to retrieve allowed rows for
	 * @return Number of allowed rows (will default to config's default)
	 * @throws IllegalStateException If the passed OfflinePlayer was not in the database, or didn't exist.
	 */
	public static int getAllowedRows(OfflinePlayer p) throws IllegalStateException {
		
		UUID uuid = p.getUniqueId();
		if (uuid == null) throw new IllegalStateException("Player must have a UUID!");
		
		User u = User.findByUUID(uuid);
		if (u == null) throw new IllegalStateException("User must exist in the database.");
		
		return getAllowedRows(u.getDBU());
		
	}
	
	/**
	 * Gets the number of rows a player is allowed using
	 * their permissions.
	 * 
	 * @param dbu DBUser to get allowed rows for.
	 * @return Number of allowed rows (will default to config's default)
	 */
	public static int getAllowedRows(DBUser dbu) {
		
		PermissionConfig config = DefianceCore.getPermissionManager().getConfig();
		int allowedRows = EnderStorage.getConfiguration().defaultRows;
		
		for (Group g : config.getGroupsByPriority(true)) {
		
			// Skip if they don't have this group...
			if (!dbu.getGroups().contains(g.name))
				continue;
			
			for (String perm : config.getPermissions(g))
				if (perm.matches("^enderstorage\\.rows\\.\\d+$")) {
					int rows = Integer.parseInt(perm.substring(perm.lastIndexOf('.') + 1));
					if (rows > allowedRows) allowedRows = rows;
				}
		}
		
		return allowedRows;
		
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
