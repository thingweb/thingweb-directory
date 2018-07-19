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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.thingweb.directory.ThingDirectory;

/**
 * 
 *  Assumes it maps to a path of the form {@code collection/*}.
 *
 * @author Victor Charpenay
 * @creation 06.02.2018
 *
 */
public abstract class CollectionItemServlet extends RESTServlet {

	private static final long serialVersionUID = 7928330177767194572L;
	
	protected static final String UUID_URN_PREFIX = "urn:uuid:";
	
	protected Collection<String> items = new HashSet<>();
	
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		items.remove(getItemID(req));
	}

	/**
	 * Should return the name of the resource newly created
	 * 
	 * @param req
	 * @param resp
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	protected String doAdd(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String id = generateItemID();
		items.add(id);
		
		return id;
	}
	
	protected Collection<String> getAllItems() {
		return items;
	}
	
	protected abstract String generateItemID();
	
	protected abstract String getItemID(HttpServletRequest req);
	
}
