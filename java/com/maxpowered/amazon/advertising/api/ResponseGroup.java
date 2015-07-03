package com.maxpowered.amazon.advertising.api;

public enum ResponseGroup {
	IMAGES("Images"),
	ITEM_ATTRIBUTES("ItemAttributes"),
	OFFERS("Offers"),
	ACCESSORIES("Accessories"),
	ALTERNATE_VERSIONS("AlternateVersions"),
	BROWSE_NODES("BrowseNodes"),
	EDITORIAL_REVIEW("EditorialReview"),
	OFFER_FULL("OfferFull"),
	OFFER_SUMMARY("OfferSummary"),
	REVIEWS("Reviews"),
	SALES_RANK("SalesRank"),
	SIMILARITIES("Similarities"),
	TRACKS("Tracks"),
	VARIATION_IMAGES("VariationImages"),
	VARIATION_MATRIX("VariationMatrix"),
	VARIATION_SUMMARY("VariationSummary"),
	VARISTIONS("Variations");

	private String name;

	ResponseGroup(final String name) {
		this.name = name;
	}

	public String getResponseGroupName() {
		return name;
	}
}
