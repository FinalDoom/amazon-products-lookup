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

package com.maxpowered.amazon.advertising.api.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.amazon.webservices.awsecommerceservice._2013_08_01.Item;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.maxpowered.amazon.advertising.api.APIRequestException;
import com.maxpowered.amazon.advertising.api.AmazonProductsAPI;
import com.maxpowered.amazon.advertising.api.ResponseGroup;
import com.maxpowered.amazon.advertising.api.processors.OutputProcessor;

/*
 * This class shows how to make a simple authenticated ItemLookup call to the Amazon Product Advertising API.
 * 
 * See the README.html that came with this sample for instructions on configuring and running the sample.
 */
public class App {
	private static final Logger LOG = LoggerFactory.getLogger(App.class);

	private static final int DEFAULT_APP_THROTTLE = 2000;
	private static final String PROPERTY_APP_THROTTLE = "app.throttle";
	private static final String PROPERTY_APP_OUTPUT = "app.output";
	private static final String PROPERTY_APP_INPUT = "app.input";
	private static final String PROCESSED_EXT = ".processed";
	private static final String DEFAULT_PROCESSED_FILE_BASE = "processedASINs" + PROCESSED_EXT;
	private static final String STD_IN_STR = "std.in";
	private static final String STD_OUT_STR = "std.out";
	public static final String DEFAULT_STR = "Defaults to ";

	public static List<String> getASINsToLookUp(final InputStream input, final FileInputStream processedFile)
			throws FileNotFoundException, IOException {
		LOG.debug("Reading ASINS from {} and excluding those in {}", input, processedFile);
		final Set<String> ret = Sets.newHashSet(IOUtils.readLines(input, StandardCharsets.UTF_8));
		// Get any ASINs already processed
		final Set<String> processed = Sets.newHashSet(IOUtils.readLines(processedFile, StandardCharsets.UTF_8));

		return Lists.newArrayList(Sets.difference(ret, processed));
	}

	public static String getOptionDefaultBasedOnSpringProperty(final AbstractApplicationContext ctx,
			final String propName, final String defaultStr) {
		String value = ctx.getBeanFactory().resolveEmbeddedValue("${" + propName + "}");

		if (value == null) {
			value = defaultStr;
		}
		return value;
	}

	public static void main(final String... args) throws FileNotFoundException, IOException, ParseException,
			JAXBException, XMLStreamException, InterruptedException {
		try (ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("application-context.xml")) {
			/*
			 * Get default options based on spring configs
			 */
			final String inputDefault = getOptionDefaultBasedOnSpringProperty(ctx, PROPERTY_APP_INPUT, STD_IN_STR);
			final String processedDefault = inputDefault.equals(STD_IN_STR) ? DEFAULT_PROCESSED_FILE_BASE
					: inputDefault + PROCESSED_EXT;
			// final String outputDefault = getOptionDefaultBasedOnSpringProperty(ctx, PROPERTY_APP_OUTPUT,
			// STD_OUT_STR);
			int throttleDefault = Integer.valueOf(getOptionDefaultBasedOnSpringProperty(ctx, PROPERTY_APP_THROTTLE,
					String.valueOf(DEFAULT_APP_THROTTLE)));
			// Maximum of 10 requests per second
			throttleDefault = Math.min(throttleDefault, DEFAULT_APP_THROTTLE);

			/*
			 * Get options from the CLI args
			 */
			final Options options = new Options();

			options.addOption("i", true, "Set the file to read ASINs from. " + DEFAULT_STR + inputDefault);
			options.addOption("p", true, "Set the file to store processed ASINs in. " + DEFAULT_STR + processedDefault);
			// Add a note that the output depends on the configured processors. If none are configured, it defaults to a
			// std.out processor
			// options.addOption("o", true, "Set the file to write fetched info xml to. " + DEFAULT_STR +
			// outputDefault);
			options.addOption("1", false, "Override output file and always output fetched info xml to std.out.");
			options.addOption("t", true, "Set the requests per second throttle (max of 10). " + DEFAULT_STR
					+ throttleDefault);

			final CommandLineParser parser = new DefaultParser();
			final CommandLine cmd = parser.parse(options, args);

			// Get input stream
			String input;
			if (cmd.hasOption("i")) {
				input = cmd.getOptionValue("i");
			} else {
				input = inputDefault;
			}
			LOG.debug("Input name is {}", input);
			// Need to read from an absolute path or /file.blah to read from classpath root
			if (!input.contains("/") && !input.contains("\\")) {
				input = "/" + input;
			}
			final InputStream inputStream = input.equals(STD_IN_STR) ? System.in :
					App.class.getClass().getResourceAsStream(input);

			// Get processed file
			String processed;
			if (cmd.hasOption("p")) {
				processed = cmd.getOptionValue("p");
			} else {
				processed = processedDefault;
			}
			LOG.debug("Processed file name is {}", processed);
			final File processedFile = new File(processed);
			processedFile.createNewFile();
			final FileInputStream processedStream = new FileInputStream(processedFile);

			// Get output stream
			// String output;
			// if (cmd.hasOption("o")) {
			// output = cmd.getOptionValue("o");
			// } else {
			// output = outputDefault;
			// }
			// final OutputStream outputStream = output.equals(STD_OUT_STR) || options.hasOption("1") ? System.out
			// : App.class.getClass().getResourceAsStream(output);

			// Get throttle rate
			final int throttle = Math.min(cmd.hasOption("t") ? Integer.valueOf(cmd.getOptionValue("t"))
					: throttleDefault, DEFAULT_APP_THROTTLE);
			LOG.debug("Throttle is {} requests per hour", throttle);
			// We don't want to hit our limit, just under an hour worth of milliseconds
			final int wait = 3540000 / throttle;

			// Get the signed requests helper
			final AmazonProductsAPI api = ctx.getBeanFactory().getBean(AmazonProductsAPI.class);
			// Get the Output processor
			final OutputProcessor output = ctx.getBeanFactory().getBean(OutputProcessor.class);

			// Search the list of remaining ASINs
			for (final String asin : getASINsToLookUp(inputStream, processedStream)) {

				// This could be easily configured through CLI or properties
				final List<String> responseGroups = Lists.newArrayList();
				for (final ResponseGroup responseGroup : new ResponseGroup[] { ResponseGroup.IMAGES,
						ResponseGroup.ITEM_ATTRIBUTES }) {
					responseGroups.add(responseGroup.getResponseGroupName());
				}
				final String responseGroupString = Joiner.on(",").join(responseGroups);

				try {
					final Item item = api.itemLookup(asin, responseGroupString);

					LOG.info("Got item titled {}", item.getItemAttributes().getTitle());
				} catch (final APIRequestException e) {
					LOG.error("Exception with API request", e);
				}

				Thread.sleep(wait);
			}
		}
	}
}
