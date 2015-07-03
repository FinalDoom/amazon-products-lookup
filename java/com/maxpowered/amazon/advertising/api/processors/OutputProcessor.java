package com.maxpowered.amazon.advertising.api.processors;

import java.util.List;

import com.google.common.collect.Lists;

public class OutputProcessor {
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
}
