package com.defiancecraft.modules.enderstorage.banks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.md_5.bungee.api.ChatColor;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import com.defiancecraft.core.api.User;
import com.defiancecraft.core.database.Database;
import com.defiancecraft.core.database.collections.Users;
import com.defiancecraft.modules.enderstorage.EnderStorage;
import com.defiancecraft.modules.enderstorage.database.collections.Banks;
import com.defiancecraft.modules.enderstorage.database.documents.DBBank;
import com.defiancecraft.modules.enderstorage.database.documents.DBBank.DBBankItem;
import com.mongodb.DBRef;

public class BankInventoryHolder implements InventoryHolder {

	private static Map<UUID, BankInventoryHolder> openInventories = new HashMap<UUID, BankInventoryHolder>();
	
	private String title;
	private int rows;
	private Inventory inventory;
	private ItemStack[] inventoryItems;
	
	private boolean saving = false;
	
	private UUID ownerUUID;
	private String ownerName;
	
	/**
	 * Constructs a BankInventoryHolder; this allows the
	 * user to browse & retrieve items from their bank,
	 * and provides the functionality of loading their
	 * items asynchronously (hence why a player is passed).
	 * 
	 * @param p Player whose bank will be opened.
	 */
	public BankInventoryHolder(Player p) {
		
		this.title = ChatColor.translateAlternateColorCodes('&', EnderStorage.getConfiguration().bankTitle);
		this.rows = BankUtils.getAllowedRows(p);
		this.inventory = Bukkit.createInventory(this, EnderStorage.getConfiguration().maxRows * 9, title);
		this.inventoryItems = new ItemStack[EnderStorage.getConfiguration().maxRows * 9];
		this.ownerUUID = p.getUniqueId();
		this.ownerName = p.getName();
		
		BankInventoryHolder.openInventories.put(ownerUUID, this);
		
	}

	public Inventory getInventory() {
		return inventory;
	}
	
	/**
	 * Updates the inventory ultimately shown to the user,
	 * adding in decoration items (where users cannot access
	 * the rows).
	 */
	public void updateInventory() {
	
		ItemStack[] shownItems = new ItemStack[EnderStorage.getConfiguration().maxRows * 9];
		System.arraycopy(inventoryItems, 0, shownItems, 0, inventoryItems.length);
		
		// Start from the first non-accessible slot, and continue
		// up until the max slot, as per the config.
		for (int i = this.rows * 9; i < EnderStorage.getConfiguration().maxRows * 9; i++) {
			shownItems[i] = EnderStorage.getConfiguration().getUpgradeItem();
		}
		
		inventory.clear();
		inventory.setContents(shownItems);
		
	}
	
	/**
	 * Loads a bank into the inventory; this does not replace existing items.
	 * If there are too few slots to support the items, they will try to
	 * be stacked. If they cannot be stacked, they are simply ignored and
	 * the returned DBBank will contain the failed items in negative slots.
	 * 
	 * Null is returned if there were no changes to the DBBank (i.e. no items
	 * were stacked or moved), and the new DBBank is returned otherwise, for
	 * saving to the DB.
	 * 
	 * @param bank Bank to load
	 * @return Null if the bank was not changed, or DBBank
	 */
	public synchronized DBBank loadBank(DBBank bank) {
		
		List<ItemStack> failed = new ArrayList<ItemStack>();
		List<DBBankItem> items = bank.getItems();
		
		for (DBBankItem item : items) {
			
			int slot = item.getSlot();
			ItemStack stack = item.toItemStack();
			
			// If the desired slot is empty, and the slot
			// is allowed.
			if (isEmpty(slot) && isAllowedSlot(slot)) {
				inventoryItems[slot] = stack;
			// otherwise, try to stack
			} else {
				ItemStack rem = putItemInEmptySlot(stack);
				if (rem != null) {
					failed.add(rem);
				}
			}
			
		}
		
		// At this point, fuck pretty slots. Just compact
		// everything and hope for the best.
		if (failed.size() > 0) {
			
			BankUtils.compactInventory(inventoryItems, rows * 9);
			List<ItemStack> newFailed = new ArrayList<ItemStack>();
			
			for (ItemStack item : failed) {
				ItemStack rem = putItemInEmptySlot(item);
				if (rem != null)
					newFailed.add(rem);
			}
			
			failed = newFailed;
			
		}
		
		this.updateInventory();

		// IOException should never be thrown, but
		// just in case it is, filter out the nulls.
		List<DBBankItem> newItems = IntStream.range(0, inventoryItems.length)
				.filter((i) -> inventoryItems[i] != null)
				.mapToObj((i) -> {
					try {
						return new DBBankItem(inventoryItems[i], i);
					} catch (IOException e) { return null; }
				})
				.filter((i) -> i != null)
				.collect(Collectors.toList());
		
		// Add the failed items (just in case they somehow
		// get more slots)
		failed.stream()
			.map((i) -> {
				try {
					return new DBBankItem(i, -1);
				} catch (IOException e) { return null; }
			})
			.filter((i) -> i != null)
			.forEach((i) -> newItems.add(i));
		
		// If the contents changed, return the new bank.
		// This is so it can be saved, as null is returned
		// otherwise.
		if (!bank.getItems().equals(newItems)) {
			bank.setItems(newItems);
			return bank;
		}
		
		return null;
		
	}
	
	/**
	 * Attempts to stack an item, or place it into an empty
	 * slot in `inventoryItems`. If there is no space, or it
	 * cannot stack, the remainder (it may have been partially
	 * stacked) is returned.
	 * 
	 * @param a ItemStack to try and put in inventory
	 * @return Remainder ItemStack, or null if it was successfully placed.
	 */
	@SuppressWarnings("deprecation")
	public ItemStack putItemInEmptySlot(ItemStack a) {
		
		// First, try to stack it.
		for (int i = 0; i < rows * 9; i++) {
			ItemStack b = inventoryItems[i];
			if (b != null
					&& b.getType().equals(a.getType())
					&& b.getDurability() == a.getDurability()
					&& b.getData().getData() == a.getData().getData()
					&& b.getAmount() < b.getMaxStackSize()
					&& ((b.getItemMeta() == null && a.getItemMeta() == null)
							|| (b.getItemMeta() != null && b.getItemMeta().equals(a.getItemMeta())))) {
				
				// If it will fit into b
				if (b.getAmount() + a.getAmount() <= b.getMaxStackSize()) {
					b.setAmount(b.getAmount() + a.getAmount());
					return null;
				// Won't fit into b.
				} else {
					// Amount needed for full stack
					int needed = b.getMaxStackSize() - b.getAmount();
					b.setAmount(b.getAmount() + needed);
					a.setAmount(a.getAmount() - needed);
				}
			}
		}
		
		// Next, find an empty slot
		for (int i = 0; i < rows * 9; i++) {
			if (inventoryItems[i] == null) {
				inventoryItems[i] = a;
				return null;
			}
		}
		
		return a;
		
	}
	
	/**
	 * Checks whether a slot is empty in the inventory
	 * 
	 * @param slot Slot to check
	 * @return Whether the slot is empty
	 */
	public boolean isEmpty(int slot) {
		return slot > -1 && inventoryItems[slot] == null;
	}
	
	/**
	 * Checks whether the player is allowed to store an
	 * item in the slot.
	 * 
	 * @param slot Slot to check
	 * @return Whether the player may store an item in the slot.
	 */
	public boolean isAllowedSlot(int slot) {
		return slot < (this.rows * 9) && slot > -1;
	}
	
	/**
	 * Checks if a slot in the BankInventoryHolder is part
	 * of the inventory.
	 * 
	 * @param slot Slot to check
	 * @return Whether the slot is in this inventory
	 */
	public boolean isBankSlot(int slot) {
		return slot >= 0 && slot < inventoryItems.length;
	}
	
	/**
	 * Checks whether the bank is busy saving
	 * @return Whether the bank is saving to DB
	 */
	public boolean isSaving() {
		return this.saving;
	}
	
	/**
	 * Opens the inventory, showing to the player asynchronously
	 * as it loads from the database.
	 */
	public void open() throws IllegalStateException {
		
		if (Bukkit.getPlayer(ownerUUID) != null
				&& this.getInventory().getViewers().contains(Bukkit.getPlayer(ownerUUID)))
			throw new IllegalStateException(ownerName + " is already viewing the BankMenu.");
		
		Player p = Bukkit.getPlayer(ownerUUID);
		if (p == null)
			throw new IllegalStateException("Player must be online to open bank.");
		
		Database.getExecutorService().submit(() -> {
		
			User user = User.findByUUIDOrCreate(ownerUUID, ownerName);
			Users users = Database.getCollection(Users.class);
			DBBank bank = Database.getCollection(Banks.class).findByUser(user.getDBU());

			// Create bank if it doesn't exist
			if (bank == null) {
				DBRef userRef = new DBRef(users.getDB(), users.getCollectionName(), user.getDBU().getId());
				bank = new DBBank(userRef, new ArrayList<DBBankItem>());
				Database.getCollection(Banks.class).createBank(bank);
			}
			
			bank = this.loadBank(bank);
			
			// Save it (should work, same ID)
			if (bank != null)
				Database.getCollection(Banks.class).save(bank);
			
			Player player = Bukkit.getPlayer(ownerUUID);
			
			if (player != null) {
				
				// Notify player if items failed to save (too many)
				if (bank != null
						&& bank.getItems().size() > this.rows * 9)
					player.sendMessage(
						ChatColor.translateAlternateColorCodes('&', EnderStorage.getConfiguration().failedMsg)
					);
				
				// Open the inv
				player.openInventory(getInventory());
				
			}
				
			
		});
		
	}
	
	/**
	 * Saves the bank. Note that once this function is called,
	 * calling methods must check that it is not already saving,
	 * or risk getting an IllegalStateException.
	 * 
	 * @return A future object; call get to wait for this method's completion.
	 */
	public Future<?> save() {
		
		if (BankInventoryHolder.openInventories.containsKey(ownerUUID)
				&& BankInventoryHolder.openInventories.get(ownerUUID).isSaving())
			throw new IllegalStateException("Bank is already saving");
		
		this.saving = true;
		
		return Database.getExecutorService().submit(() -> {
			
			User user = User.findByUUIDOrCreate(ownerUUID, ownerName);
			DBBank bank = Database.getCollection(Banks.class).findByUser(user.getDBU());
			
			//
			// Convert ItemStack[] to List<DBBankItem>
			//
			List<DBBankItem> items = new ArrayList<DBBankItem>(bank.getItems());
			ItemStack[] newItems = getInventory().getContents();
			
			// IMPORTANT: must only go up to rows * 9, or else
			// duplication glitch will occur (dummy items will be
			// duped)
			for (int i = 0; i < rows * 9; i++) {
				
				// Remove it regardless of whether null or not
				final int j = i;
				items = items.stream().filter((item) -> item.getSlot() != j).collect(Collectors.toList());
				
				// Add it back, if not null
				if (newItems[i] != null)
					try {
						items.add(new DBBankItem(newItems[i], i));
					} catch (IOException e) {}
			}
			
			// Save it
			bank.setItems(items);
			Database.getCollection(Banks.class).save(bank);
			
			// Remove from map so they can re-open
			if (BankInventoryHolder.openInventories.containsKey(ownerUUID))
				BankInventoryHolder.openInventories.remove(ownerUUID);
			
		});
		
	}
	
	/**
	 * Checks whether a user is viewing a bank already (this will
	 * remain true until it has successfully saved).
	 * 
	 * @param uuid UUID of user
	 * @return Whether the user is viewing a bank
	 */
	public static boolean isUserViewingBank(UUID uuid) {
		return BankInventoryHolder.openInventories.containsKey(uuid);
	}

	/**
	 * Saves all open banks synchronously
	 * 
	 * @throws InterruptedException If the saving thread was interrupted
	 * @throws ExecutionException If the saving thread failed to execute
	 */
	public static void saveAllBanks() throws InterruptedException, ExecutionException {

		List<Future<?>> futures = new ArrayList<Future<?>>();
		
		for (Entry<UUID, BankInventoryHolder> entry : BankInventoryHolder.openInventories.entrySet()) {
			
			futures.add(entry.getValue().save());
			
			if (Bukkit.getPlayer(entry.getKey()) != null)
				Bukkit.getPlayer(entry.getKey()).closeInventory();
			
		}
		
		for (Future<?> f : futures)
			f.get();
		
	}
	
}
