package com.defiancecraft.modules.enderstorage.database.collections;

import com.defiancecraft.core.database.collections.Collection;
import com.defiancecraft.core.database.documents.DBUser;
import com.defiancecraft.modules.enderstorage.database.documents.DBBank;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

public class Banks extends Collection {

	public String getCollectionName() {
		return "banks";
	}

	public DBBank findByUser(DBUser u) {
		DBObject obj = getDBC().findOne(new BasicDBObject("user.$id", u.getId()));
		return obj == null ? null : new DBBank(obj);
	}
	
	public WriteResult createBank(DBBank bank) {
		return this.save(bank);
	}
	
}
