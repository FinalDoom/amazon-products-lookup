/**********************************************************************************************
 * Copyright 2009 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "LICENSE.txt" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * ********************************************************************************************
 *
 * Amazon Product Advertising API Signed Requests Sample Code
 *
 * API Version: 2009-03-31
 *
 */

package com.amazon.advertising.api.sample;

import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.amazon.webservices.awsecommerceservice._2013_08_01.Item;
import com.maxpowered.amazon.advertising.api.SignedRequestsHelper;

/*
 * This class shows how to make a simple authenticated ItemLookup call to the Amazon Product Advertising API.
 * 
 * See the README.html that came with this sample for instructions on configuring and running the sample.
 */
public class ItemLookupSample {
	/*
	 * Version of the webservices API. Grabs this from the target/generated/cfx package name
	 */
	private static String VERSION;
	static {
		final String packageName = Item.class.getPackage().getName();
		final String[] packageComponents = packageName.split("\\.");
		VERSION = packageComponents[packageComponents.length - 1].substring(1).replaceAll("_", "-");
	}

	/*
	 * The Item ID to lookup. The value below was selected for the US locale. You can choose a different value if this
	 * value does not work in the locale of your choice.
	 */
	private static final String ITEM_ID = "0545010225";

	public static void main(final String[] args) {
		/*
		 * Get the signed requests helper
		 */
		try (ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("application-context.xml")) {
			final SignedRequestsHelper helper = ctx.getBean(SignedRequestsHelper.class);

			String requestUrl = null;
			String title = null;

			/* The helper can sign requests in two forms - map form and string form */

			/*
			 * Here is an example in map form, where the request parameters are stored in a map.
			 */
			System.out.println("Map form example:");
			final Map<String, String> params = new HashMap<String, String>();
			params.put("Service", "AWSECommerceService");
			params.put("Version", VERSION);
			params.put("Operation", "ItemLookup");
			params.put("ItemId", ITEM_ID);
			params.put("ResponseGroup", "Small");

			requestUrl = helper.sign(params);
			System.out.println("Signed Request is \"" + requestUrl + "\"");

			title = fetchTitle(requestUrl);
			System.out.println("Signed Title is \"" + title + "\"");
			System.out.println();

			/*
			 * Here is an example with string form, where the requests parameters have already been concatenated into a
			 * query string.
			 */
			System.out.println("String form example:");
			final String queryString = "Service=AWSECommerceService&Version=" + VERSION
					+ "&Operation=ItemLookup&ResponseGroup=Small&ItemId=" + ITEM_ID;
			requestUrl = helper.sign(queryString);
			System.out.println("Request is \"" + requestUrl + "\"");

			title = fetchTitle(requestUrl);
			System.out.println("Title is \"" + title + "\"");
			System.out.println();
		}
	}

	/*
	 * Utility function to fetch the response from the service and extract the title from the XML.
	 */
	private static String fetchTitle(final String requestUrl) {
		String title = null;
		try {
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final Document doc = db.parse(requestUrl);
			final Node titleNode = doc.getElementsByTagName("Title").item(0);
			title = titleNode.getTextContent();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
		return title;
	}

}
