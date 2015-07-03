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
 * API Version: 2013-08-01
 *
 */

package com.maxpowered.amazon.advertising.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * This class contains all the logic for signing requests to the Amazon Product Advertising API.
 */
public class SignedRequestsHelper {
	private static final Logger LOG = LoggerFactory.getLogger(SignedRequestsHelper.class);

	/**
	 * All strings are handled as UTF-8
	 */
	public static final String UTF8_CHARSET = "UTF-8";

	/**
	 * The HMAC algorithm required by Amazon
	 */
	private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

	/**
	 * This is the URI for the service, don't change unless you really know what you're doing.
	 */
	private static final String REQUEST_URI = "/onca/xml";

	/**
	 * The sample uses HTTP GET to fetch the response. If you changed the sample to use HTTP POST instead, change the
	 * value below to POST.
	 */
	private static final String REQUEST_METHOD = "GET";

	private static Unmarshaller unmarshaller;
	static {
		try {
			unmarshaller = JAXBContext.newInstance(Constants.API_PACKAGE).createUnmarshaller();
		} catch (final JAXBException ex) {
			LOG.error("Error creating unmarshaller", ex);
		}
	}

	private final String endpoint;
	private final String associateTag;
	private final String awsAccessKeyId;
	private final String awsSecretKey;

	private final SecretKeySpec secretKeySpec;
	private final Mac mac;

	/**
	 * You must provide the four values below to initialize the helper. This should be done through spring, however.
	 *
	 * @param endpoint
	 *            String name of an endpoint
	 * @param associateTag
	 *            Your AWS Associate Tag
	 * @param awsAccessKeyId
	 *            Your AWS Access Key ID
	 * @param awsSecretKey
	 *            Your AWS Secret Key
	 * @throws NoSuchAlgorithmException
	 *             if the hashing algorithm is invalid (never)
	 * @throws InvalidKeyException
	 *             if the secret key is invalid
	 * @throws UnsupportedEncodingException
	 *             if the encoding charset is invalid
	 */
	@Autowired
	SignedRequestsHelper(@Value("${aws.endpoint}") final String endpoint,
			@Value("${aws.associateTag}") final String associateTag,
			@Value("${aws.accessKeyId}") final String awsAccessKeyId,
			@Value("${aws.secretKey}") final String awsSecretKey) throws NoSuchAlgorithmException, InvalidKeyException,
			UnsupportedEncodingException {
		this(Endpoint.valueOf(endpoint), associateTag, awsAccessKeyId, awsSecretKey);
	}

	/**
	 * You must provide the four values below to initialize the helper.
	 *
	 * @param endpoint
	 *            Destination for the requests.
	 * @param associateTag
	 *            Your AWS Associate Tag
	 * @param awsAccessKeyId
	 *            Your AWS Access Key ID
	 * @param awsSecretKey
	 *            Your AWS Secret Key
	 * @throws NoSuchAlgorithmException
	 *             if the hashing algorithm is invalid (never)
	 * @throws InvalidKeyException
	 *             if the secret key is invalid
	 * @throws UnsupportedEncodingException
	 *             if the encoding charset is invalid
	 */
	SignedRequestsHelper(final Endpoint endpoint, final String associateTag, final String awsAccessKeyId,
			final String awsSecretKey) throws NoSuchAlgorithmException, InvalidKeyException,
			UnsupportedEncodingException {
		if (null == endpoint || endpoint.getAPIdomain().length() == 0) {
			throw new IllegalArgumentException("endpoint is null or empty");
		}
		if (null == associateTag || associateTag.length() == 0) {
			throw new IllegalArgumentException("awsAssociateTag is null or empty");
		}
		if (null == awsAccessKeyId || awsAccessKeyId.length() == 0) {
			throw new IllegalArgumentException("awsAccessKeyId is null or empty");
		}
		if (null == awsSecretKey || awsSecretKey.length() == 0) {
			throw new IllegalArgumentException("awsSecretKey is null or empty");
		}

		this.endpoint = endpoint.getAPIdomain().toLowerCase();
		this.associateTag = associateTag;
		this.awsAccessKeyId = awsAccessKeyId;
		this.awsSecretKey = awsSecretKey;

		final byte[] secretyKeyBytes = this.awsSecretKey.getBytes(UTF8_CHARSET);
		secretKeySpec = new SecretKeySpec(secretyKeyBytes, HMAC_SHA256_ALGORITHM);
		mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
		mac.init(secretKeySpec);
	}

	/**
	 * This method signs requests in hashmap form. It returns a URL that should be used to fetch the response. The URL
	 * returned should not be modified in any way, doing so will invalidate the signature and Amazon will reject the
	 * request.
	 */
	public String sign(final Map<String, String> params) {
		// Let's add the AWSAccessKeyId and Timestamp parameters to the request.
		params.put("AssociateTag", associateTag);
		params.put("AWSAccessKeyId", awsAccessKeyId);
		params.put("Timestamp", timestamp());
		params.put("Version", Constants.API_VERSION);

		// The parameters need to be processed in lexicographical order, so we'll
		// use a TreeMap implementation for that.
		final SortedMap<String, String> sortedParamMap = new TreeMap<String, String>(params);

		// get the canonical form the query string
		final String canonicalQS = canonicalize(sortedParamMap);

		// create the string upon which the signature is calculated
		final String toSign = REQUEST_METHOD + "\n" + endpoint + "\n" + REQUEST_URI + "\n" + canonicalQS;

		// get the signature
		final String hmac = hmac(toSign);
		final String sig = percentEncodeRfc3986(hmac);

		// construct the URL
		final String url = "http://" + endpoint + REQUEST_URI + "?" + canonicalQS + "&Signature=" + sig;

		return url;
	}

	/**
	 * This method signs requests in query-string form. It returns a URL that should be used to fetch the response. The
	 * URL returned should not be modified in any way, doing so will invalidate the signature and Amazon will reject the
	 * request.
	 */
	public String sign(final String queryString) {
		// let's break the query string into it's constituent name-value pairs
		final Map<String, String> params = createParameterMap(queryString);

		// then we can sign the request as before
		return this.sign(params);
	}

	/**
	 * Compute the HMAC.
	 *
	 * @param stringToSign
	 *            String to compute the HMAC over.
	 * @return base64-encoded hmac value.
	 */
	private String hmac(final String stringToSign) {
		String signature = null;
		byte[] data;
		byte[] rawHmac;
		try {
			data = stringToSign.getBytes(UTF8_CHARSET);
			rawHmac = mac.doFinal(data);
			final Base64 encoder = new Base64();
			signature = new String(encoder.encode(rawHmac));
		} catch (final UnsupportedEncodingException e) {
			throw new RuntimeException(UTF8_CHARSET + " is unsupported!", e);
		}
		return signature;
	}

	/**
	 * Generate a ISO-8601 format timestamp as required by Amazon.
	 *
	 * @return ISO-8601 format timestamp.
	 */
	private String timestamp() {
		String timestamp = null;
		final Calendar cal = Calendar.getInstance();
		final DateFormat dfm = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		dfm.setTimeZone(TimeZone.getTimeZone("GMT"));
		timestamp = dfm.format(cal.getTime());
		return timestamp;
	}

	/**
	 * Canonicalize the query string as required by Amazon.
	 *
	 * @param sortedParamMap
	 *            Parameter name-value pairs in lexicographical order.
	 * @return Canonical form of query string.
	 */
	private String canonicalize(final SortedMap<String, String> sortedParamMap) {
		if (sortedParamMap.isEmpty()) {
			return "";
		}

		final StringBuffer buffer = new StringBuffer();
		final Iterator<Map.Entry<String, String>> iter = sortedParamMap.entrySet().iterator();

		while (iter.hasNext()) {
			final Map.Entry<String, String> kvpair = iter.next();
			buffer.append(percentEncodeRfc3986(kvpair.getKey()));
			buffer.append("=");
			buffer.append(percentEncodeRfc3986(kvpair.getValue()));
			if (iter.hasNext()) {
				buffer.append("&");
			}
		}
		final String cannoical = buffer.toString();
		return cannoical;
	}

	/**
	 * Percent-encode values according the RFC 3986. The built-in Java URLEncoder does not encode according to the RFC,
	 * so we make the extra replacements.
	 *
	 * @param s
	 *            decoded string
	 * @return encoded string per RFC 3986
	 */
	private String percentEncodeRfc3986(final String s) {
		String out;
		try {
			out = URLEncoder.encode(s, UTF8_CHARSET).replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
		} catch (final UnsupportedEncodingException e) {
			out = s;
		}
		return out;
	}

	/**
	 * Takes a query string, separates the constituent name-value pairs and stores them in a hashmap.
	 *
	 * @param queryString
	 * @return
	 */
	private Map<String, String> createParameterMap(final String queryString) {
		final Map<String, String> map = new HashMap<String, String>();
		final String[] pairs = queryString.split("&");

		for (final String pair : pairs) {
			if (pair.length() < 1) {
				continue;
			}

			final String[] tokens = pair.split("=", 2);
			for (int j = 0; j < tokens.length; j++) {
				try {
					tokens[j] = URLDecoder.decode(tokens[j], UTF8_CHARSET);
				} catch (final UnsupportedEncodingException e) {
				}
			}
			switch (tokens.length) {
				case 1: {
					if (pair.charAt(0) == '=') {
						map.put("", tokens[0]);
					} else {
						map.put(tokens[0], "");
					}
					break;
				}
				case 2: {
					map.put(tokens[0], tokens[1]);
					break;
				}
			}
		}
		return map;
	}

	public <T> T fetchAndUnmarshal(final Map<String, String> params, final Class<T> clazz) throws IOException,
			JAXBException, XMLStreamException {
		return unmarshal(fetch(params), clazz);
	}

	public InputStream fetch(final Map<String, String> params) throws IOException {
		if (!params.containsKey("Operation")) {
			throw new RuntimeException("params must have an Operation");
		}

		// Sign the params in a URL as Amazon specifies.
		final String urlString = sign(params);
		LOG.info("Got signed request url string {}", urlString);
		final URL url = new URL(urlString);
		return url.openStream();
	}

	public <T> T unmarshal(final InputStream responseStream, final Class<T> clazz) throws JAXBException,
			XMLStreamException, IOException {
		T response;
		try {
			response = clazz.cast(unmarshaller.unmarshal(responseStream));
		} finally {
			responseStream.close();
		}

		return response;
	}
}
