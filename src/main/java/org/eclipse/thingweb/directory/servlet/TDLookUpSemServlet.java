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
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.thingweb.directory.ThingDirectory;
import org.eclipse.thingweb.directory.rest.RESTServlet;
import org.eclipse.thingweb.directory.servlet.utils.BufferedResponseWrapper;
import org.eclipse.thingweb.directory.servlet.utils.RedirectedRequestWrapper;
import org.eclipse.thingweb.directory.sparql.client.Queries;

public class TDLookUpSemServlet extends RESTServlet {
	
	private static final String QUERY_PARAMETER = "query";
	
	// TODO add CoRE Link format
	private static final String[] ACCEPTED = { "application/json" };
	
	protected final TDServlet tdServlet;
	
	public TDLookUpSemServlet(TDServlet servlet) {
		tdServlet = servlet;
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		super.doGet(req, resp);
		
		if (resp.isCommitted()) {
			return; // parent class returned an error status
		}
		
		String query = req.getParameter(QUERY_PARAMETER);
		if (query == null) {
			query = "?s ?p ?o";
		}
		
		Set<String> filtered = new HashSet<>();
		try (TupleQueryResult res = Queries.listResources(query)) {
			while (res.hasNext()) {
				String uri = res.next().getValue("res").stringValue();
				String id = uri.substring(4); // TODO harmonize with RDFDocumentServlet

				filtered.add(id);
			}
		}

		OutputStream out = resp.getOutputStream();
		try {
			out.write('{');
			
			Iterator<String> it = filtered.iterator();
			while (it.hasNext()) {
				String id = it.next();

				// FIXME get actual servlet mapping if not 'td/{id}'?
				RedirectedRequestWrapper reqWrapper = new RedirectedRequestWrapper(req, "td/" + id);
				BufferedResponseWrapper respWrapper = new BufferedResponseWrapper(resp);
				tdServlet.doGet(reqWrapper, respWrapper);
				
				if (respWrapper.getStatus() < HttpServletResponse.SC_BAD_REQUEST) {
					out.write('\"');
					out.write(id.getBytes());
					out.write('\"');
					out.write(':');
					out.write(respWrapper.getOutputBytes());
					if (it.hasNext()) {
						out.write(',');
					}
				} else {
					ThingDirectory.LOG.warn("Trying to access non-existing (or expired) TD: " + id);
				}
			}
			out.write('}');
			resp.setContentType("application/json");
			out.close(); // sends response
		} catch (IOException e) {
			ThingDirectory.LOG.error("Cannot write byte array", e);
			resp.sendError(500, e.getMessage()); // Internal Server Error
		}
		
		// TODO basic SPARQL-based text search
		// TODO add filter pattern
	}

	@Override
	protected String[] getAcceptedContentTypes() {
		return ACCEPTED;
	}

}
