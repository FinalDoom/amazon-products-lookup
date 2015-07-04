package com.maxpowered.amazon.advertising.api;

import com.amazon.webservices.awsecommerceservice._2013_08_01.Errors;

public class APIRequestException extends Exception {
	private static final long serialVersionUID = 4943079295157496363L;

	public APIRequestException(final Errors.Error error) {
		super("Error returned by API: [code] " + error.getCode() + " [message] " + error.getMessage());
	}

}
