package com.maxpowered.amazon.advertising.api;

import com.amazon.webservices.awsecommerceservice._2013_08_01.Item;

public class Constants {
	public static String API_PACKAGE;
	public static String API_VERSION;
	static {
		API_PACKAGE = Item.class.getPackage().getName();
		final String[] packageComponents = API_PACKAGE.split("\\.");
		API_VERSION = packageComponents[packageComponents.length - 1].substring(1).replaceAll("_", "-");
	}
}
