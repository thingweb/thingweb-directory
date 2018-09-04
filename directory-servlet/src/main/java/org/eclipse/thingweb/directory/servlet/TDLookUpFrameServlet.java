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
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.thingweb.directory.ResourceManager;
import org.eclipse.thingweb.directory.ResourceManagerFactory;
import org.eclipse.thingweb.directory.vocabulary.TD;

public class TDLookUpFrameServlet extends HttpServlet {
	
	private static final long serialVersionUID = 5679530570591631536L;
	
	public final static String DEFAULT_MEDIA_TYPE = "application/td+json";
	
	public static final String DEFAULT_FRAME = "{ \"@context\": \"" + TD.NAMESPACE + "\", \"@type\": \"Thing\" }";

	private static final String LOOKUP_FRAME_TYPE = "frame";
	
	private static final String FRAME_PARAMETER = "frame";
	
	private final ResourceManager manager = ResourceManagerFactory.get("td");
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String contentType = req.getHeader("Accept");
		if (contentType == null) contentType = DEFAULT_MEDIA_TYPE;
		
		Map<String, String> parameters = new HashMap<>();
		// TODO
		
		String query = req.getParameter(FRAME_PARAMETER);
		if (query == null) query = DEFAULT_FRAME;
		
		resp.setContentType(contentType);
		manager.lookUp(LOOKUP_FRAME_TYPE, resp.getOutputStream(), contentType, query, parameters);
	}

}
