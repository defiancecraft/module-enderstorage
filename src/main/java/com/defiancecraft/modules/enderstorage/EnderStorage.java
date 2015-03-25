package com.defiancecraft.modules.enderstorage;

import java.util.concurrent.ExecutionException;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.defiancecraft.core.database.collections.Collection;
import com.defiancecraft.core.modules.Module;
import com.defiancecraft.modules.enderstorage.banks.BankInventoryHolder;
import com.defiancecraft.modules.enderstorage.database.collections.Banks;
import com.defiancecraft.modules.enderstorage.listeners.EnderChestListener;
import com.defiancecraft.modules.enderstorage.utils.EnderStorageConfig;

public class EnderStorage extends JavaPlugin implements Module {

	private static EnderStorageConfig config = null;
	
	@Override
	public void onEnable() {
		
		EnderStorage.config = getConfig(EnderStorageConfig.class);
		
		PluginManager manager = getServer().getPluginManager();
		manager.registerEvents(new EnderChestListener(), this);
		
	}
	
	@Override
	public void onDisable() {
	
		getLogger().info("Saving all open banks...");
		
		try {
			BankInventoryHolder.saveAllBanks();
			getLogger().info("Open banks saved!");
		} catch (InterruptedException | ExecutionException e) {
			getLogger().severe("Error while saving banks! Stack trace below.");
			e.printStackTrace();
		}

	}
	
	public static EnderStorageConfig getConfiguration() {
		return EnderStorage.config;
	}
	
	@Override
	public String getCanonicalName() {
		return getDescription().getName();
	}

	@Override
	public Collection[] getCollections() {
		return new Collection[] { new Banks() };
	}

}
