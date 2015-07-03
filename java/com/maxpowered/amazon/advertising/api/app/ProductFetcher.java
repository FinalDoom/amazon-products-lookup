package com.maxpowered.amazon.advertising.api.app;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazon.webservices.awsecommerceservice._2013_08_01.Errors;
import com.amazon.webservices.awsecommerceservice._2013_08_01.Item;
import com.amazon.webservices.awsecommerceservice._2013_08_01.ItemLookupResponse;
import com.amazon.webservices.awsecommerceservice._2013_08_01.Request;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.maxpowered.amazon.advertising.api.APIRequestException;
import com.maxpowered.amazon.advertising.api.APIResponseException;
import com.maxpowered.amazon.advertising.api.AmazonProductsAPI;
import com.maxpowered.amazon.advertising.api.processors.OutputProcessor;

public class ProductFetcher implements AutoCloseable {
	private static final Logger LOG = LoggerFactory.getLogger(ProductFetcher.class);
	private static final int THROTTLE_MAX_RETRIES = 3;

	private int throttledRetries = 0;
	private FileOutputStream processedFileOutputStream;
	private FileInputStream processedFileInputStream;
	private String responseGroups;
	private InputStream inputStream;
	private int requestWait;

	private final AmazonProductsAPI api;
	private final OutputProcessor outputProcessor;

	private final Set<String> successfulAsins = Sets.newHashSet();
	private final Set<String> attemptedAsins = Sets.newHashSet();

	@Autowired
	ProductFetcher(final AmazonProductsAPI api, final OutputProcessor outputProcessor) {
		this.api = api;
		this.outputProcessor = outputProcessor;
	}

	public void setProcessedFile(final File file) throws FileNotFoundException {
		processedFileOutputStream = new FileOutputStream(file);
		processedFileInputStream = new FileInputStream(file);
	}

	public void setResponseGroups(final String responseGroups) {
		this.responseGroups = responseGroups;
	}

	public void setInputStream(final InputStream inputStream) {
		this.inputStream = inputStream;
	}

	public void setRequestWait(final int requestWait) {
		this.requestWait = requestWait;
	}

	public Set<String> getASINsToLookUp()
			throws FileNotFoundException, IOException {
		LOG.debug("Reading ASINS from {} and excluding those in {}", inputStream, processedFileInputStream);
		final Set<String> ret = Sets.newHashSet(IOUtils.readLines(inputStream, StandardCharsets.UTF_8));
		LOG.info("Got {} input ASINs", ret.size());
		// Get any ASINs already processed
		final Set<String> processed = Sets.newHashSet(IOUtils.readLines(processedFileInputStream,
				StandardCharsets.UTF_8));
		LOG.info("Got {} processed ASINs", processed.size());

		return Sets.difference(ret, processed);
	}

	public void recordProcessed(final List<String> asins)
			throws IOException {
		final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(processedFileOutputStream));
		for (final String asin : asins) {
			writer.write(asin);
			writer.write(System.lineSeparator());
		}
		writer.flush();
	}

	public void fetchProductInformation() throws FileNotFoundException, IOException, JAXBException, XMLStreamException {
		// Search the list of remaining ASINs
		final Set<String> asins = getASINsToLookUp();

		final List<String> asinGroup = Lists.newArrayListWithCapacity(10);
		for (final String asin : asins) {
			asinGroup.add(asin);

			if (asinGroup.size() == 10) {
				if (lookUpAsinGroup(asinGroup)) {
					break;
				}
			}
		}
		lookUpAsinGroup(asinGroup);

		LOG.info("Successfully retrieved {} ASINs", successfulAsins.size());
		final Set<String> failedAsins = Sets.difference(attemptedAsins, successfulAsins);
		LOG.info("Failed to retrieve {} ASINs", failedAsins.size());
		LOG.debug("Failed to retrieve ASINSs: {}", failedAsins);
		LOG.info("Success rate: {} / {} = {}%", successfulAsins.size(), attemptedAsins.size(),
				(double) successfulAsins.size() / attemptedAsins.size());
		LOG.info("Processed asins {} / {} = {}%", attemptedAsins.size(), asins.size(),
				(double) attemptedAsins.size() / asins.size());

	}

	public boolean lookUpAsinGroup(final List<String> asinGroup) throws IOException, JAXBException, XMLStreamException {
		LOG.debug("Looking up ASINs {}", asinGroup);
		ItemLookupResponse response;
		try {
			response = api.itemLookup(Joiner.on(",").join(asinGroup), responseGroups);
		} catch (final APIResponseException e1) {
			// Retry logic in case the throttling is too high
			LOG.error("Probable throttling response, waiting extra time", e1);
			asinGroup.clear();
			throttledRetries++;
			if (throttledRetries > THROTTLE_MAX_RETRIES) {
				return true;
			}
			try {
				Thread.sleep(requestWait * (throttledRetries + 1));
			} catch (final InterruptedException e) {
				LOG.error("Interrupted!", e);
				return true;
			}
			return false;
		}

		final Request itemRequest = response.getItems().get(0).getRequest();
		if (itemRequest.getErrors() != null) {
			for (final Errors.Error error : itemRequest.getErrors().getError()) {
				LOG.error("Exception with API request for an item", new APIRequestException(error));
			}
		}

		try {
			for (final Item item : response.getItems().get(0).getItem()) {
				LOG.debug("Got item titled {}", item.getItemAttributes().getTitle());
				successfulAsins.add(item.getASIN());
				outputProcessor.writeItem(item);
			}
		} catch (final Exception e) {
			LOG.error("Error getting items", e);
		}

		recordProcessed(asinGroup);
		attemptedAsins.addAll(asinGroup);
		asinGroup.clear();
		try {
			throttledRetries = 0;
			Thread.sleep(requestWait);
		} catch (final InterruptedException e) {
			LOG.error("Interrupted!", e);
			return true;
		}
		return false;
	}

	@Override
	public void close() throws Exception {
		if (processedFileInputStream != null) {
			processedFileInputStream.close();
		}
		if (processedFileOutputStream != null) {
			processedFileOutputStream.close();
		}
	}
}
