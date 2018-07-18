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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.thingweb.directory.rest.CollectionServlet;

public class CoAPServletWrapper extends CoapResource {

	private final HttpServlet servlet;
	
	public CoAPServletWrapper(String name, HttpServlet servlet) {
		super(name);
		this.servlet = servlet;
	}
	
	@Override
	public void handleRequest(Exchange exchange) {
		CoAPServletRequest req = new CoAPServletRequest(exchange.getRequest());
		CoAPServletResponse resp = new CoAPServletResponse();
		try {
			servlet.service(req, resp);
			
			if (resp.getStatus() == HttpServletResponse.SC_CREATED) {
				String loc = resp.getHeader(CollectionServlet.LOCATION_HEADER);
				add(new CoAPServletWrapper(loc, ((CollectionServlet) servlet).getItemServlet()));
			}
			
			exchange.sendResponse(resp.asResponse());
		} catch (ServletException | IOException e) {
			// TODO
		}
	}
	
}
