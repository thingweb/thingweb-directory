package de.thingweb.directory.coap;

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

import de.thingweb.directory.ThingDirectory;

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
