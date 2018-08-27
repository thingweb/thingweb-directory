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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.eclipse.thingweb.directory.ResourceManager;
import org.eclipse.thingweb.directory.ResourceManagerFactory;

public abstract class ManagerRelatedServlet extends HttpServlet {
	
	private static final long serialVersionUID = -296910367591425649L;

	public final static String REGISTRATION_TYPE_INIT_PARAM = "registration-type";
	
	protected ResourceManager manager;

	@Override
	public void init() throws ServletException {
		throw new ServletException(REGISTRATION_TYPE_INIT_PARAM + " init param is mandatory");
	}
	
	@Override
	public void init(ServletConfig config) throws ServletException {		
		String reg = config.getInitParameter(REGISTRATION_TYPE_INIT_PARAM);
		if (reg == null) {
			throw new ServletException(REGISTRATION_TYPE_INIT_PARAM + " init param is mandatory");
		} else {
			manager = ResourceManagerFactory.get(reg);
		}
	}
	
}
