package com.maxpowered.amazon.advertising.api.processors;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.webservices.awsecommerceservice._2013_08_01.Item;
import com.google.common.collect.Lists;

public class OutputProcessor implements Processor {
	private static final Logger LOG = LoggerFactory.getLogger(OutputProcessor.class);

	public List<Processor> processors;

	public void setProcessors(final List<Processor> processors) {
		this.processors = processors;
	}

	public void addProcessor(final Processor processor) {
		if (processors == null) {
			processors = Lists.newArrayList();
		}
		processors.add(processor);
	}

	@Override
	public void writeItem(final Item item) {
		for (final Processor processor : processors) {
			try {
				processor.writeItem(item);
			} catch (final Exception e) {
				LOG.error("Error writing to processor: {}", processor, e);
			}
		}
	}
}
