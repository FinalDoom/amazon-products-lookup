package com.maxpowered.amazon.advertising.api.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.webservices.awsecommerceservice._2013_08_01.Item;

public class MySqlProcessor implements Processor {
	private static final Logger LOG = LoggerFactory.getLogger(MySqlProcessor.class);

	@Override
	public void writeItem(final Item item) {
		// TODO Auto-generated method stub
		LOG.info("MySQLProcessor logging item: {}", item);
	}

}
