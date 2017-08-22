package com.defiancecraft.modules.enderstorage.utils;

public class CompatibilityException extends RuntimeException {

	private static final long serialVersionUID = -569174978406621352L;
	private final String msg;
	
	public CompatibilityException() {
		this("Feature is not available due to an incompatibility between the server item storage and the server version.");
	}
	
	public CompatibilityException(String msg) {
		this.msg = msg;
	}
	
	public String getMessage() {
		return msg;
	}
	
}
