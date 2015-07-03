package com.maxpowered.amazon.advertising.api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.amazon.webservices.awsecommerceservice._2013_08_01.ItemLookupResponse;
import com.amazon.webservices.awsecommerceservice._2013_08_01.ItemSearchResponse;

/**
 * Finds products through the Amazon Products API.
 *
 * Operations supported so far are: ItemLookup, ItemSearch.
 */
public class AmazonProductsAPI {
	private static final Logger LOG = LoggerFactory.getLogger(AmazonProductsAPI.class);
	private final SignedRequestsHelper helper;
	private final boolean logFullResponse;

	@Autowired
	public AmazonProductsAPI(final SignedRequestsHelper helper,
			@Value("${app.logFullResponse}") final boolean logFullResponse) {
		this.helper = helper;
		this.logFullResponse = logFullResponse;
	}

	/**
	 * Do an ItemLookup request for multiple items
	 *
	 * @see http://docs.amazonwebservices.com/AWSECommerceService/2010-09-01/DG/index.html?ItemLookup.html
	 *
	 * @param responseGroups
	 *            Comma-seperated response groups.
	 * @throws JAXBException
	 * @throws IOException
	 * @throws XMLStreamException
	 * @throws APIResponseException
	 * @throws APIRequestException
	 */
	public ItemLookupResponse itemLookup(final String asin, final String responseGroups) throws JAXBException,
			XMLStreamException, IOException, APIResponseException {
		final Map<String, String> params = new HashMap<String, String>();
		params.put("Operation", "ItemLookup");
		params.put("ItemId", asin);
		params.put("ResponseGroup", responseGroups);

		final ItemLookupResponse response = getResponseItem(params, ItemLookupResponse.class);
		return response;
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
	 * @throws APIRequestException
	 * @throws APIResponseException
	 */
	public ItemSearchResponse itemSearch(final String query, final String responseGroup, final String searchIndex)
			throws IOException, JAXBException, XMLStreamException, APIRequestException, APIResponseException {
		final Map<String, String> params = new HashMap<String, String>();
		params.put("SearchIndex", searchIndex);
		params.put("Operation", "ItemSearch");
		params.put("Keywords", query);
		params.put("ResponseGroup", responseGroup);

		final ItemSearchResponse response = getResponseItem(params, ItemSearchResponse.class);
		return response;
		/*
		 * This is how you get the items. Not used right now, so just move this logic wherever it gets used. See the
		 * usage of the above method
		 */
		// final Request itemRequest = response.getItems().get(0).getRequest();
		// if (itemRequest.getErrors() != null) {
		// throw new APIRequestException(itemRequest.getErrors().getError().get(0));
		// }
		// if (response != null && response.getItems() != null && response.getItems().size() > 0
		// && response.getItems().get(0).getItem() != null) {
		// return response.getItems().get(0).getItem();
		// }
		// return null;
	}

	private <T> T getResponseItem(final Map<String, String> params, final Class<T> responseClass) throws JAXBException,
			XMLStreamException, IOException, APIResponseException {
		try {
			if (LOG.isDebugEnabled() || logFullResponse) {
				final byte[] responseBytes = IOUtils.toByteArray(helper.fetch(params));
				if (logFullResponse) {
					LOG.info("Got ItemLookupResponse {}", new String(responseBytes, StandardCharsets.UTF_8));
				} else {
					LOG.debug("Got ItemLookupResponse {}", new String(responseBytes, StandardCharsets.UTF_8));
				}
				return helper.unmarshal(new ByteArrayInputStream(responseBytes), responseClass);
			} else {
				return helper.fetchAndUnmarshal(params, responseClass);
			}
		} catch (final IOException e) {
			throw new APIResponseException("API returned a non-200 response code", e);
		}
	}
}
