package com.maxpowered.util;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.webservices.awsecommerceservice._2013_08_01.Item;
import com.amazon.webservices.awsecommerceservice._2013_08_01.Price;
import com.maxpowered.amazon.advertising.api.SignedRequestsHelper;

public class Utils {
	private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

	// No longer used
	public static InputStream replaceStringFromStream(final InputStream in, final String find, final String replacement) {
		InputStream ret = null;
		try {
			String content = IOUtils.toString(in, SignedRequestsHelper.UTF8_CHARSET);
			content = content.replaceAll(find, replacement);
			ret = IOUtils.toInputStream(content, SignedRequestsHelper.UTF8_CHARSET);
		} catch (final IOException ex) {
			LOG.error("Error streaming response", ex);
		}
		return ret;
	}

	/**
	 * Helper method to find the "normal" (or best guess at normal) price of an item.
	 */
	public static Price getPrice(final Item item) {
		Price price = null;

		// Get Amazon's list price.
		try {
			if ((price = item.getItemAttributes().getListPrice()) != null) {
				return price;
			}
		} catch (final NullPointerException ex) {
		}

		// Get the lowest new price from a third-party.
		try {
			if ((price = item.getOfferSummary().getLowestNewPrice()) != null) {
				return price;
			}
		} catch (final NullPointerException ex) {
		}

		return price;
	}
}
