package com.maxpowered.amazon.advertising.api;

import java.io.IOException;

public class APIResponseException extends Exception {
	private static final long serialVersionUID = -5915884724183412163L;

	public APIResponseException(final String msg) {
		super(msg);
	}

	public APIResponseException(final String msg, final IOException error) {
		super(msg, error);
	}

}
