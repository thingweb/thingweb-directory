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
import java.io.OutputStream;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.thingweb.directory.ThingDirectory;

public class CollectionServlet extends RESTServlet {

	public static final String LOCATION_HEADER = "Location";
	
	private static final String[] ACCEPTED = { "application/json" };
	
	protected final CollectionItemServlet itemServlet;
	
	public CollectionServlet(CollectionItemServlet child) {
		itemServlet = child;
	}
	
	public CollectionItemServlet getItemServlet() {
		return itemServlet;
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		super.doGet(req, resp);
		
		OutputStream out = resp.getOutputStream();
		try {
			out.write('[');
			
			Iterator<String> it = itemServlet.getAllItems().iterator();
			while (it.hasNext()) {
				out.write('"');
				out.write(it.next().getBytes());
				out.write('"');
				if (it.hasNext()) {
					out.write(',');
				}
			}
			
			out.write(']');
		} catch (IOException e) {
			ThingDirectory.LOG.error("Cannot write byte array", e);
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		int delta = itemServlet.getAllItems().size();
		String id = itemServlet.doAdd(req, resp);
		delta = itemServlet.getAllItems().size() - delta;
		
		if (delta > 0) {
			resp.setStatus(HttpServletResponse.SC_CREATED);
		} else {
			ThingDirectory.LOG.info("Item already registered: " + id);
			resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
		}
		
		resp.setHeader(LOCATION_HEADER, id);
	}
	
	@Override
	protected String[] getAcceptedContentTypes() {
		return ACCEPTED;
	}

}
