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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 
 * Performs standard REST processing like content negotiation
 * and linking.
 *
 * @author Victor Charpenay
 * @creation 06.02.2018
 *
 */
public abstract class RESTServlet extends HttpServlet {
	
	private static final long serialVersionUID = -4241859870694994605L;
	
	protected static final String ACCEPT_HEADER = "Accept";
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String ct = req.getHeader(ACCEPT_HEADER);
		if (ct != null && !acceptsContentType(ct)) {
			resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
		}
	}

	// TODO use Servlet Filters instead?
	protected abstract String[] getAcceptedContentTypes();
	
	private boolean acceptsContentType(String ct) {
		String regex = ct.replaceAll("[*]", ".*");
		for (String accepted : getAcceptedContentTypes()) {
			if (accepted.matches(regex)) {
				return true;
			}
		}
		return false;
	}
	
}
