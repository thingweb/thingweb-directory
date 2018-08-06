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
package org.eclipse.thingweb.directory.rest;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Performs content-negotiation.
 * 
 *  Expects the config parameter {@code "accepted"},
 *  as a comma-separated value of content types.
 *
 * @author Victor Charpenay
 * @creation 03.08.2018
 *
 */
public class ContentNegotiationFilter implements Filter {

	public static final String FILTER_PARAMETER = "accepted";
	
	public static final String ACCEPT_HEADER = "Accept";
	
	private final Set<String> acceptedContentTypes = new HashSet<>();
	
	@Override
	public void destroy() {
		// do nothing
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
		String accept = ((HttpServletRequest) req).getHeader(ACCEPT_HEADER);
		if (accept != null && acceptedContentTypes.contains(accept)) {
			// TODO process patterns too
			chain.doFilter(req, resp);
		} else {
			((HttpServletResponse) resp).sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
		}
	}

	@Override
	public void init(FilterConfig config) throws ServletException {
		String csv = config.getInitParameter(FILTER_PARAMETER);
		
		if (csv != null) {
			for (String ct : csv.split(",")) {
				acceptedContentTypes.add(ct);
			}
		}
	}

}
