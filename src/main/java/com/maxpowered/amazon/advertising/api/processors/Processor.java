package com.maxpowered.amazon.advertising.api.processors;

import com.amazon.webservices.awsecommerceservice._2013_08_01.Item;

public interface Processor {
	void writeItem(Item item) throws Exception;
}
