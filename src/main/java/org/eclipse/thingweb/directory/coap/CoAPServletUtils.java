/********************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
 * 
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the W3C Software Notice and
 * Document License (2015-05-13) which is available at
 * https://www.w3.org/Consortium/Legal/2015/copyright-software-and-document.
 * 
 * SPDX-License-Identifier: EPL-2.0 OR W3C-20150513
 ********************************************************************************/
package org.eclipse.thingweb.directory.coap;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.californium.core.coap.CoAP.Code;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.thingweb.directory.ThingDirectory;

public class CoAPServletUtils {
	
	public static InputStream payload(CoapExchange exchange) {
	  return new ByteArrayInputStream(exchange.getRequestPayload());
	}

	public static int toContentFormatCode(String contentType) {
		if (contentType.equals("application/ld+json")) {
			contentType = "application/json";
		}
    	return MediaTypeRegistry.parse(contentType);
	}
	
	public static String toContentType(int contentFormatCode) {
		switch (contentFormatCode) {
			default: return "text/plain";
		}
	}
	
	public static Map<String, Set<String>> toHeaders(OptionSet options) {
		return null; // TODO
	}
	
	/**
	 * Parameters are expected to be key/value pairs separated by '='
	 * @param CoAP URI query option
	 * @return
	 */
	public static Map<String, String[]> toParameters(List<String> uriQuery) {
		Map<String, Set<String>> params = new HashMap<>();

		try {
			for (String pair : uriQuery) {
				pair = URLDecoder.decode(pair, "UTF-8");
				if (pair.contains("=")) {
					String[] p = pair.split("=");
					
					if (!params.containsKey(p[0])) {
						params.put(p[0], new HashSet<>());
					}
					
					if (p.length > 1) {
						params.get(p[0]).add(p[1]);
					} else {
						params.get(p[0]).add("");
					}
				}
			}
		} catch (UnsupportedEncodingException e) {
			ThingDirectory.LOG.warn("UTF-8 encoding not supported", e);
		}
		
		Map<String, String[]> map = new HashMap<>();
		params.forEach((key, set) -> {
			map.put(key, (String[]) set.toArray());
		});
		
		return map;
	}
	
	public static ResponseCode toResponseCode(int status) {
		return null; // TODO
	}

}
