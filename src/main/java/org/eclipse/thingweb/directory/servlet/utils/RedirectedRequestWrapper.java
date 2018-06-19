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
package org.eclipse.thingweb.directory.servlet.utils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * 
 * Wraps the input request, up to the requested path.
 *
 * @author Victor Charpenay
 * @creation 07.02.2018
 *
 */
public class RedirectedRequestWrapper extends HttpServletRequestWrapper {

	private String path;
	
	public RedirectedRequestWrapper(HttpServletRequest req, String redirectedTo) {
		super(req);
		path = redirectedTo;
	}
	
	@Override
	public String getRequestURI() {
		return path;
	}
	
}
