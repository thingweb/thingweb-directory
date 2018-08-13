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
package org.eclipse.thingweb.directory.filter;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

/**
 * 
 * .
 *
 * @author Victor Charpenay
 * @creation 12.08.2018
 *
 */
@WebFilter(
	filterName="ContentNegotiation",
	servletNames={"RegistrationHandle"},
	urlPatterns={"/td/*", "/vocab/*"},
	initParams={
		@WebInitParam(
			name="accepted",
			value="application/n-triples,text/turtle,application/rdf+xml,application/ld+json,application/td+json"
		)
	}
)
public class ContentNegotiationFilter implements Filter {

	/**
	 * Comma-separated list of IANA registered media types. E.g. "text/plain,text/html"
	 */
	public static final String ACCEPTED_CONFIG_PARAMETER = "accepted";
	
	private static final String FALLBACK_CONTENT_TYPE = "*/*";
	
	private final Set<String> acceptedContentTypes = new HashSet<String>();
	
	public ContentNegotiationFilter() {
		acceptedContentTypes.add(FALLBACK_CONTENT_TYPE);
	}
	
	public void destroy() {
		// nothing to do
	}

	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
		if (req instanceof HttpServletRequest && resp instanceof HttpServletResponse) {
			String accept = (((HttpServletRequest) req).getHeader("Accept"));
			Set<String> cts = new HashSet<>();
			
			for (String entry : accept.split(",")) {
				String ct = entry.split(";")[0]; // ignoring other parameters
				cts.add(ct);
			}
			
			cts.retainAll(acceptedContentTypes);
			
			if (cts.isEmpty()) {
				((HttpServletResponse) resp).sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
			} else {
				HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper((HttpServletRequest) req);
				cts.removeIf(str -> str.contains("*"));
				if (cts.isEmpty()) {
					// TODO remove accept header
				} else {
					// TODO replace accept header
				}
			}
		}
		
		chain.doFilter(req, resp);
	}

	public void init(FilterConfig config) throws ServletException {
		String blankRegex = "\\s";
		String[] accepted = config.getInitParameter(ACCEPTED_CONFIG_PARAMETER).replaceAll(blankRegex, "").split(",");
		
		for (String ct : accepted) {
			if (!ct.matches("\\w+/[\\w\\+-]+")) {
				throw new ServletException("Content negotiation init parameter is not correct");
			}
			
			acceptedContentTypes.add(ct);
		}
	}

}
