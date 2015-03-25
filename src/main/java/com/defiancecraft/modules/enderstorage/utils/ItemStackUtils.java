package com.defiancecraft.modules.enderstorage.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.inventory.meta.ItemMeta;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * Utility class for handling ItemStacks and
 * related objects.
 */
public class ItemStackUtils {

	/**
	 * Opposite to {@link #deserializeMap(Map)}
	 * 
	 * @see #deserializeMap(Map)
	 */
	@SuppressWarnings("unchecked")
	private static Map<String, Object> serializeMap(Map<String, Object> map) {
		
		for (Entry<String, Object> entry : map.entrySet()) {
			
			//---
			// Recursively serialize anything that's already a map (in case
			// it contains items that aren't serial)
			//---
			if (entry.getValue() instanceof Map)
				entry.setValue(serializeMap((Map<String, Object>)entry.getValue()));
			
			//---
			// Attempt to serialize every item in list
			//---
			if (entry.getValue() instanceof List) {
				
				List<Object> newList = new ArrayList<Object>();
				for (Object o : (List<?>)entry.getValue())
					
					// Convert every ConfigurationSerializable to a Map, and serialize that map.
					if (o instanceof ConfigurationSerializable) {
						Map<String, Object> serializedValue = new HashMap<String, Object>(((ConfigurationSerializable)o).serialize());
						serializedValue.put(ConfigurationSerialization.SERIALIZED_TYPE_KEY, ConfigurationSerialization.getAlias((Class<? extends ConfigurationSerializable>)o.getClass()));
						newList.add(serializeMap(serializedValue));
					// Serialize every map in the list. 
					} else if (o instanceof Map) {
						newList.add(serializeMap((Map<String, Object>)o));
					// Otherwise, just add the object (it doesn't need to be serialized further... probably. Or can't be.)
					} else {
						newList.add(o);
					}
				entry.setValue(newList);
				
			}
			
			//---
			// Attempt to serialize the value itself if it's serializable.
			//---
			if (entry.getValue() instanceof ConfigurationSerializable) {
				
				Map<String, Object> serializedValue = new HashMap<String, Object>(((ConfigurationSerializable)entry.getValue()).serialize());
				serializedValue.put(ConfigurationSerialization.SERIALIZED_TYPE_KEY, ConfigurationSerialization.getAlias((Class<? extends ConfigurationSerializable>)entry.getValue().getClass()));
				entry.setValue(serializedValue);
				
			}
			
		}
		
		return map;
	}
	
	/**
	 * Further deserializes an already serialized ConfigurationSerializable;
	 * the current ConfigurationSerialization class does not serialize recursively,
	 * i.e. there can exist entries with classes as they are (not in Map form). These
	 * must be deserialized to be used with BSON.
	 * 
	 * @param map A serialized ConfigurationSerializable
	 * @return The completely deserialized thing
	 */
	@SuppressWarnings("unchecked")
	private static Map<String, Object> deserializeMap(Map<String, Object> map) {
		
		for (Entry<String, Object> entry : map.entrySet()) {
			
			//---
			// If it's a Map, attempt to deserialize as is (could be serialized ConfigurationSerializable)
			// If that fails, just deserialize the whole map (i.e. deserialize its contents)
			//---
			if (entry.getValue() instanceof Map)
				try {
					// Attempt to deserialize as Bukkit thing
					entry.setValue(ConfigurationSerialization.deserializeObject(deserializeMap((Map<String, Object>)entry.getValue())));
				} catch (Exception e) {
					// Try deserializing tree if not ConfigurationSerializable
					entry.setValue(deserializeMap((Map<String, Object>)entry.getValue()));
				}
			
			
			//---
			// If it's a List, attempt to deserialize every item in the list
			// Set the value to a new list; could contain the same as original list,
			// or might have deserialized objects instead. All maps are deserialized.
			//---
			if (entry.getValue() instanceof List) {
				
				List<Object> newList = new ArrayList<Object>();
				for (Object o : (List<?>)entry.getValue())
					if (o instanceof Map)
						try {
							newList.add(ConfigurationSerialization.deserializeObject(deserializeMap((Map<String, Object>)o)));
						} catch (Exception e) {
							newList.add(deserializeMap((Map<String, Object>)o));
						}
					else
						newList.add(o);
				
				entry.setValue(newList);
				
			}
				
			
		}

		return map;
		
	}
	
	/**
	 * Serializes ItemMeta into a DBObject, for storage in a DB.
	 * 
	 * @param meta ItemMeta to serialize
	 * @return Serialized DBObject, or null if it failed.
	 */
	public static DBObject serializeItemMeta(ItemMeta meta) {
		
		try {
			// Shallow copy, as ItemMeta#serialize() is an ImmutableMap
			Map<String, Object> map = new HashMap<String, Object>(meta.serialize());
			
			// Serialize the map (BSON cannot store non BSON types)
			map = serializeMap(map);
			
			// This type key is required for deserialization by ConfigurationSerialization
			map.put(ConfigurationSerialization.SERIALIZED_TYPE_KEY, "ItemMeta");
			
			return new BasicDBObject(map);
		} catch (Exception e) {
			Bukkit.getLogger().warning("Failed to serialize ItemMeta! Stack trace below.");
			e.printStackTrace();
			return null;
		}
		
	}
	
	/**
	 * Deserializes a DBObject into ItemMeta
	 * 
	 * @param data DBObject to deserialize
	 * @return Deserialized ItemMeta, or null if it failed.
	 */
	@SuppressWarnings("unchecked")
	public static ItemMeta deserializeItemMeta(DBObject data) {
		
		try {
			return (ItemMeta) ConfigurationSerialization.deserializeObject(deserializeMap(data.toMap()));
		} catch (Exception e) {
			Bukkit.getLogger().warning("Failed to deserialize ItemMeta! Stack trace below");
			e.printStackTrace();
			return null;
		}
		
	}
	
}
