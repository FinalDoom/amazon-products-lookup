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
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.maxpowered.amazon.advertising.api.ResponseGroup;
import com.maxpowered.amazon.advertising.api.processors.FileProcessor;

/*
 * This class shows how to make a simple authenticated ItemLookup call to the Amazon Product Advertising API.
 *
 * See the README.html that came with this sample for instructions on configuring and running the sample.
 */
public class App {
	private static final Logger LOG = LoggerFactory.getLogger(App.class);

	private static final int MAX_APP_THROTTLE = 25000;
	private static final int DEFAULT_APP_THROTTLE = 2000;
	private static final String PROPERTY_APP_THROTTLE = "app.throttle";
	private static final String PROPERTY_APP_OUTPUT = "app.output";
	private static final String PROPERTY_APP_INPUT = "app.input";
	private static final String PROCESSED_EXT = ".processed";
	private static final String DEFAULT_PROCESSED_FILE_BASE = "processedASINs" + PROCESSED_EXT;
	private static final String STD_IN_STR = "std.in";
	private static final String STD_OUT_STR = "std.out";
	private static final String DEFAULT_STR = "Defaults to ";

	public static String getOptionDefaultBasedOnSpringProperty(final AbstractApplicationContext ctx,
			final String propName, final String defaultStr) {
		String value = ctx.getBeanFactory().resolveEmbeddedValue("${" + propName + "}");

		if (value == null) {
			value = defaultStr;
		}
		return value;
	}

	public static void main(final String... args) throws FileNotFoundException, IOException,
			JAXBException, XMLStreamException, InterruptedException {
		try (ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("application-context.xml")) {
			/*
			 * Get default options based on spring configs
			 */
			final String inputDefault = getOptionDefaultBasedOnSpringProperty(ctx, PROPERTY_APP_INPUT, STD_IN_STR);
			final String processedDefault = inputDefault.equals(STD_IN_STR) ? DEFAULT_PROCESSED_FILE_BASE
					: inputDefault + PROCESSED_EXT;
			final String outputDefault = getOptionDefaultBasedOnSpringProperty(ctx, PROPERTY_APP_OUTPUT,
					STD_OUT_STR);
			int throttleDefault = Integer.valueOf(getOptionDefaultBasedOnSpringProperty(ctx, PROPERTY_APP_THROTTLE,
					String.valueOf(DEFAULT_APP_THROTTLE)));
			// Maximum of 25000 requests per hour
			throttleDefault = Math.min(throttleDefault, MAX_APP_THROTTLE);

			/*
			 * Get options from the CLI args
			 */
			final Options options = new Options();

			options.addOption("h", false, "Display this help.");
			options.addOption("i", true, "Set the file to read ASINs from. " + DEFAULT_STR + inputDefault);
			options.addOption("p", true, "Set the file to store processed ASINs in. " + DEFAULT_STR + processedDefault
					+ " or '" + PROCESSED_EXT + "' appended to the input file name.");
			// Add a note that the output depends on the configured processors. If none are configured, it defaults to a
			// std.out processor
			options.addOption("o", true, "Set the file to write fetched info xml to via FileProcessor. " + DEFAULT_STR +
					outputDefault);
			options.addOption("1", false, "Override output file and always output fetched info xml to std.out.");
			options.addOption("t", true, "Set the requests per hour throttle (max of " + MAX_APP_THROTTLE + "). "
					+ DEFAULT_STR + throttleDefault);

			final CommandLineParser parser = new DefaultParser();
			CommandLine cmd = null;
			boolean needsHelp = false;

			try {
				cmd = parser.parse(options, args);
			} catch (final ParseException e) {
				needsHelp = true;
			}

			if (cmd.hasOption("h") || needsHelp) {
				final HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("App", options);
				return;
			}

			// Get throttle rate
			final int throttle = Math.min(cmd.hasOption("t") ? Integer.valueOf(cmd.getOptionValue("t"))
					: throttleDefault, MAX_APP_THROTTLE);
			LOG.debug("Throttle (default {}) is {} requests per hour", throttleDefault, throttle);
			// We don't want to hit our limit, just under an hour worth of milliseconds
			final int requestWait = 3540000 / throttle;

			// Get input stream
			String input;
			if (cmd.hasOption("i")) {
				input = cmd.getOptionValue("i");
			} else {
				input = inputDefault;
			}
			LOG.debug("Input name (default {}) is {}", inputDefault, input);

			// Get processed file
			String processed;
			if (cmd.hasOption("p")) {
				processed = cmd.getOptionValue("p");
			} else {
				processed = input + PROCESSED_EXT;
			}
			LOG.debug("Processed file name (default {}) is {}", processedDefault, processed);
			final File processedFile = new File(processed);
			processedFile.createNewFile();

			try (
					final InputStream inputStream = getInputStream(input)) {

				// Get output stream
				String output;
				if (cmd.hasOption("o")) {
					output = cmd.getOptionValue("o");
				} else {
					output = outputDefault;
				}
				if (cmd.hasOption("1")) {
					output = STD_OUT_STR;
				}
				LOG.debug("Output (default {}) name is {}", outputDefault, output);
				// Special logic to set the FileProcessor output
				if (output.equals(STD_OUT_STR)) {
					final FileProcessor fileProcessor = ctx.getBeanFactory().getBean(FileProcessor.class);
					fileProcessor.setOutputStream(System.out);
				} else if (!output.equals(outputDefault)) {
					final FileProcessor fileProcessor = ctx.getBeanFactory().getBean(FileProcessor.class);
					fileProcessor.setOutputFile(output);
				}

				// This could be easily configured through CLI or properties
				final List<String> responseGroups = Lists.newArrayList();
				for (final ResponseGroup responseGroup : new ResponseGroup[] { ResponseGroup.IMAGES,
						ResponseGroup.ITEM_ATTRIBUTES }) {
					responseGroups.add(responseGroup.getResponseGroupName());
				}
				final String responseGroupString = Joiner.on(",").join(responseGroups);

				// Search the list of remaining ASINs
				final ProductFetcher fetcher = ctx.getBeanFactory().getBean(ProductFetcher.class);
				fetcher.setProcessedFile(processedFile);
				fetcher.setRequestWait(requestWait);
				fetcher.setInputStream(inputStream);
				fetcher.setResponseGroups(responseGroupString);

				// This ensures that statistics of processed asins should almost always get printed at the end
				Runtime.getRuntime().addShutdownHook(new Thread() {
					@Override
					public void run() {
						fetcher.logStatistics();
					}
				});

				fetcher.fetchProductInformation();
			}
		}
	}

	private static InputStream getInputStream(final String input) throws FileNotFoundException {
		InputStream inputStream;
		if (input.equals(STD_IN_STR)) {
			inputStream = System.in;
		} else {
			String classInput = input;
			// Need to read from an absolute path or /file.blah to read from classpath root
			if (!input.contains("/") && !input.contains("\\")) {
				classInput = "/" + input;
			}
			inputStream = App.class.getClass().getResourceAsStream(classInput);
			if (inputStream == null) {
				inputStream = new FileInputStream(input);
			}
		}

		return inputStream;
	}
}
