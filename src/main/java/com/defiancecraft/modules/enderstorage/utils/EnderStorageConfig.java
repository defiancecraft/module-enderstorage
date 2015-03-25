package com.defiancecraft.modules.enderstorage.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class EnderStorageConfig {

	public String bankTitle = "&1Bank";
	public int bankId = 0;
	public int defaultRows = 3;
	public int maxRows = 10;
	public String upgradeItem = "STAINED_GLASS_PANE:14";
	public String upgradeText = "&aUpgrade to unlock more slots!";
	public String failedMsg = "&cYour bank is full, but your items are safe. Upgrade to get more slots, or remove some items.";
	
	public ItemStack getUpgradeItem() {
		
		Material type = Material.getMaterial(upgradeItem.split(":")[0]);
		ItemStack ret = new ItemStack(type);
		
		// Set durability if item has it set (e.g. for coloured glass)
		if (upgradeItem.indexOf(":") > -1) {
			ret.setDurability((short)(Integer.parseInt(upgradeItem.split(":")[1]) & 0xFFFF));
		}
		
		// Set item's name
		if (upgradeText != null && !upgradeText.isEmpty()) {
			ItemMeta meta = ret.getItemMeta();
			meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', upgradeText));
			ret.setItemMeta(meta);
		}
		
		return ret;
		
	}

}
