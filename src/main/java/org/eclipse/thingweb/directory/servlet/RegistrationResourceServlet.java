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
package org.eclipse.thingweb.directory.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.thingweb.directory.ThingDirectory;
import org.eclipse.thingweb.directory.rest.CollectionItemServlet;

/**
 * 
 * Implements registration resource operations from the
 * CoRE Resource Directory specification (Sect. 5.4).
 *
 * @author Victor Charpenay
 * @creation 06.02.2018
 *
 */
public abstract class RegistrationResourceServlet extends CollectionItemServlet {

	private static final long serialVersionUID = -1803741562937257698L;

	/**
	 * Default lifetime: 24h (86,400s)
	 */
	public final static Integer DEFAULT_LIFETIME = 86400;
	
	public static final String PARAMETER_LIFETIME = "lt";
	
	public static final String PARAMETER_ENDPOINT = "ep";
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String id = getItemID(req);
		if (hasExpired(id)) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
		
		// TODO implement CoRE Link format representation
	}
	
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (req.getParameter(PARAMETER_LIFETIME) != null) {
			int lt = Integer.parseInt(req.getParameter(PARAMETER_LIFETIME));
			updateTimeout(getItemID(req), lt);
		}
	}
	
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO ?
		super.doDelete(req, resp);
	}
	
	protected String getBaseURI(HttpServletRequest req) {
		// TODO take ep into account
		return ThingDirectory.getBaseURI() + "/";
	}
	
	protected abstract boolean hasExpired(String id);
	
	protected abstract void updateTimeout(String id, int lifetime);

}
