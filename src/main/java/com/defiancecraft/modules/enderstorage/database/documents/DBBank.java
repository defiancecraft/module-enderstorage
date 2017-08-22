package com.defiancecraft.modules.enderstorage.database.documents;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;

import com.defiancecraft.core.database.documents.Document;
import com.defiancecraft.modules.enderstorage.utils.CompatibilityException;
import com.defiancecraft.modules.enderstorage.utils.ItemStackUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

public class DBBank extends Document {

	public static final String FIELD_USER = "user";
	public static final String FIELD_ITEMS = "items";
	
	public DBBank(DBObject obj) {
		super(obj);
	}
	
	public DBBank(DBRef user, List<DBBankItem> items) {
		super(new BasicDBObject());
		getDBO().put(FIELD_USER, user);
		getDBO().put(FIELD_ITEMS,
				items.stream()
					.map((i) -> i.getDBO()).collect(Collectors.toList()));
	}
	
	public DBRef getUser() {
		return getDBRef(FIELD_USER);
	}
	
	public List<DBBankItem> getItems() {
		return getDBObjectList(FIELD_ITEMS)
			.stream()
			.map((i) -> new DBBankItem(i))
			.collect(Collectors.toList());
	}
	
	public void setItems(List<DBBankItem> items) {
		getDBO().put(FIELD_ITEMS, items
			.stream()
			.map((i) -> i.getDBO())
			.collect(Collectors.toList()));
	}
	
	public static class DBBankItem extends Document {

		public static final String FIELD_TYPE = "type";
		public static final String FIELD_AMOUNT = "amount";
		public static final String FIELD_SLOT = "slot";
		public static final String FIELD_DATA = "data";
		public static final String FIELD_META = "meta";
		public static final String FIELD_DAMAGE = "damage";
		
		public DBBankItem(DBObject obj) {
			super(obj);
		}
		
		@SuppressWarnings("deprecation")
		public DBBankItem(ItemStack item, int slot) throws IOException {
			super(new BasicDBObject());
			getDBO().put(FIELD_TYPE, item.getType().toString());
			getDBO().put(FIELD_AMOUNT, item.getAmount());
			getDBO().put(FIELD_SLOT, slot);
			getDBO().put(FIELD_DATA, item.getData().getData());
			getDBO().put(FIELD_DAMAGE, item.getDurability());
			
			// Putting it in unnecessarily wastes sooooo much space, so check.
			if (item.hasItemMeta())
				getDBO().put(FIELD_META, ItemStackUtils.serializeItemMeta(item.getItemMeta()));
		}
		
		public Material getType() {
			return Material.getMaterial(getString(FIELD_TYPE));
		}
		
		public int getSlot() {
			return getInt(FIELD_SLOT);
		}
		
		public byte getData() {
			return getByte(FIELD_DATA, (byte)0);
		}
		
		public ItemMeta getItemMeta() {
			return getDBO().containsField(FIELD_META) ? 
					ItemStackUtils.deserializeItemMeta(getDBObject(FIELD_META)) :
					null;
		}
		
		public short getDamage() {
			return (short)(getInt(FIELD_DAMAGE, 0) & 0xFFFF);
		}
		
		public int getAmount() {
			return getInt(FIELD_AMOUNT, 1);
		}
		
		/**
		 * Converts the DBBankItem to an ItemStack. 
		 * @return ItemStack
		 * @throws Exception If the DBBankItem cannot be converted to an ItemStack (for compatibility reasons, etc.)
		 */
		@SuppressWarnings("deprecation")
		public ItemStack toItemStack() throws CompatibilityException {
			
			if (getType() == null)
				throw new CompatibilityException("Material of stored ItemStack is not supported");
			
			ItemStack ret = new ItemStack(getType(), getAmount());
			if (getDBO().containsField(FIELD_DATA)) ret.setData(new MaterialData(getType(), getData()));
			if (getDBO().containsField(FIELD_DAMAGE)) ret.setDurability(getDamage());
			if (getDBO().containsField(FIELD_META)
					&& getItemMeta() != null) ret.setItemMeta(getItemMeta());
			
			return ret;
			
		}
		
	}
	
}
