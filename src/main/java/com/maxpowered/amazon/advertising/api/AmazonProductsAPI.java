package com.maxpowered.amazon.advertising.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazon.webservices.awsecommerceservice._2013_08_01.Item;
import com.amazon.webservices.awsecommerceservice._2013_08_01.ItemLookupResponse;
import com.amazon.webservices.awsecommerceservice._2013_08_01.ItemSearchResponse;

/**
 * Finds products through the Amazon Products API.
 *
 * Operations supported so far are: ItemLookup, ItemSearch.
 */
public class AmazonProductsAPI {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(AmazonProductsAPI.class);

	@Autowired
	private SignedRequestsHelper helper;

	/**
	 * Do an ItemLookup request.
	 *
	 * @see http://docs.amazonwebservices.com/AWSECommerceService/2010-09-01/DG/index.html?ItemLookup.html
	 *
	 * @param responseGroups
	 *            Comma-seperated response groups.
	 * @throws JAXBException
	 * @throws IOException
	 * @throws XMLStreamException
	 */
	public Item itemLookup(final String asin, final String responseGroups) throws IOException, JAXBException,
			XMLStreamException {
		final Map<String, String> params = new HashMap<String, String>();
		params.put("Operation", "ItemLookup");
		params.put("ItemId", asin);
		params.put("ResponseGroup", responseGroups);

		final ItemLookupResponse response = helper.fetchAndUnmarshal(params, ItemLookupResponse.class);
		return response.getItems().get(0).getItem().get(0);
	}

	/**
	 * Do an ItemLookup request with customizable ResponseGroup.
	 *
	 * @see http://docs.amazonwebservices.com/AWSECommerceService/2010-09-01/DG/index.html?ItemSearch.html
	 *
	 * @param searchIndex
	 *            "All" or one of the following:
	 *            'All','Apparel','Automotive','Baby','Beauty','Blended','Books','Classical','DVD',
	 *            'DigitalMusic','Electronics','GourmetFood','Grocery','HealthPersonalCare','HomeGarden',
	 *            'Industrial','Jewelry','KindleStore','Kitchen','MP3Downloads','Magazines','Merchants',
	 *            'Miscellaneous','Music','MusicTracks','MusicalInstruments','OfficeProducts',
	 *            'OutdoorLiving','PCHardware','PetSupplies','Photo','Shoes','SilverMerchants',
	 *            'Software','SportingGoods','Tools','Toys','UnboxVideo','VHS','Video','VideoGames',
	 *            'Watches','Wireless','WirelessAccessories'.
	 * @throws JAXBException
	 * @throws IOException
	 * @throws XMLStreamException
	 */
	public List<Item> itemSearch(final String query, final String responseGroup, final String searchIndex)
			throws IOException, JAXBException, XMLStreamException {
		final Map<String, String> params = new HashMap<String, String>();
		params.put("SearchIndex", searchIndex);
		params.put("Operation", "ItemSearch");
		params.put("Keywords", query);
		params.put("ResponseGroup", responseGroup);

		final ItemSearchResponse response = helper.fetchAndUnmarshal(params, ItemSearchResponse.class);
		return response.getItems().get(0).getItem();
	}
}
