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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.eclipse.thingweb.directory.ThingDirectory;
import org.eclipse.thingweb.directory.rest.RESTServlet;
import org.eclipse.thingweb.directory.servlet.exception.MalformedDocumentException;
import org.eclipse.thingweb.directory.sparql.client.Queries;

public class RDFDocumentServlet extends RegistrationResourceServlet {

	private static final long serialVersionUID = -5515251842793698062L;

	public final static String DEFAULT_FORMAT = "JSON-LD";

	public final static RDFFormat DEFAULT_RDF_FORMAT = RDFFormat.JSONLD;
	
	public final static String DEFAULT_MEDIA_TYPE = "application/ld+json";
	
	private final static String[] ACCEPTED = {
		"application/n-triples",
		"text/turtle",
		"application/rdf+xml",
		"application/ld+json"
	};

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		super.doGet(req, resp);

		String uri = getItemID(req);
		Model m = Queries.getResource(uri);
		
		if (!m.isEmpty()) {
			writeContent(m, req, resp);
		} else {
			items.remove(uri);
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}
	
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		super.doPut(req, resp);
		
		try {
			Model m = Rio.parse(req.getInputStream(), getBaseURI(req), getContentFormat(req));

			String uri = getItemID(req);
			Queries.replaceResource(uri, m);
		} catch (RDFParseException | UnsupportedRDFormatException | IOException e) {
			throw new ServletException(e);
		}
	}
	
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		super.doDelete(req, resp);

		String uri = getItemID(req);
		Queries.deleteResource(uri);
		
		ThingDirectory.LOG.info("Deleted RDF document: " + uri);
	}
	
	@Override
	protected String doAdd(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			Model m = readContent(req, resp);
			
			String uri = generateItemID(m);
			
			if (!items.contains(uri)) {
				Queries.loadResource(uri, m);
				
				// TODO normalize RDF graph (by giving URIs to blank nodes)

				items.add(uri);
				ThingDirectory.LOG.info(String.format("Added RDF document: %s (%d triples)", uri, m.size()));
			}
			
			return uri;
		} catch (MalformedDocumentException e) {
			ThingDirectory.LOG.error("Malformed RDF document", e);
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
			return null;
		} catch (RDFParseException | UnsupportedRDFormatException | IOException e) {
			ThingDirectory.LOG.error("Cannot parse RDF document", e);
			
			throw new ServletException(e);
		}
	}
	
	/**
	 * @return by default, random UUID URN (see https://tools.ietf.org/html/rfc4122)
	 */
	@Override
	protected String generateItemID() {
		return UUID_URN_PREFIX + UUID.randomUUID();
	}

	/**
	 * Assumes resource name is a URL-encoded URI
	 */
	@Override
	protected String getItemID(HttpServletRequest req) {
		String uri = req.getRequestURI();
		String id = uri.substring(uri.lastIndexOf("/") + 1, uri.length());
		try {
			return URLDecoder.decode(id, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			ThingDirectory.LOG.warn("Cannot URL-decode resource identifier: " + id, e);
			return null;
		}
	}
	
	/**
	 * Alternative to generateItemID()
	 * 
	 * @param m
	 * @return
	 * @throws MalformedDocumentException 
	 */
	protected String generateItemID(Model m) throws MalformedDocumentException {
		return generateItemID();
	}
	
	@Override
	protected boolean hasExpired(String id) {
		return Queries.hasExpired(id);
	}

	@Override
	protected void updateTimeout(String id, int lifetime) {
		Queries.updateTimeout(id, lifetime);
	}
	
	protected RDFFormat getContentFormat(HttpServletRequest req) {
		RDFFormat format = DEFAULT_RDF_FORMAT;
		
		if (req.getContentType() != null) {
			String mediaType = req.getContentType();
			// TODO guess RDF specific type from generic media type (CoAP)
			format = Rio.getParserFormatForMIMEType(mediaType).orElse(format);
		}
		
		return format;
	}
	
	protected RDFFormat getAcceptedFormat(HttpServletRequest req) {
		RDFFormat format = DEFAULT_RDF_FORMAT;
		
		if (req.getHeader(RESTServlet.ACCEPT_HEADER) != null) {
			String mediaType = req.getHeader(RESTServlet.ACCEPT_HEADER);
			format = Rio.getParserFormatForMIMEType(mediaType).orElse(format);
		}
		
		return format;
	}
	
	/**
	 * To be overridden by subclasses if pre-processing is required (e.g. RDF lifting)
	 * 
	 * @param req servlet request
	 * @param resp servlet response
	 * @return RDF document as an RDF4J model
	 * @throws RDFParseException
	 * @throws UnsupportedRDFormatException
	 * @throws IOException
	 */
	protected Model readContent(HttpServletRequest req, HttpServletResponse resp) throws RDFParseException, UnsupportedRDFormatException, IOException {
		return Rio.parse(req.getInputStream(), getBaseURI(req), getContentFormat(req));
	}
	
	/**
	 * To be overridden by subclasses if post-processing is required (e.g. JSON-LD framing).
	 * 
	 * @param m RDF document as an RDF4J model
	 * @param req servlet request
	 * @param resp servlet response
	 * @throws RDFHandlerException
	 * @throws IOException
	 */
	protected void writeContent(Model m, HttpServletRequest req, HttpServletResponse resp) throws RDFHandlerException, IOException {
		RDFFormat format = getAcceptedFormat(req);
		resp.setContentType(format.getDefaultMIMEType());
		Rio.write(m, resp.getOutputStream(), format);
	}
	
	@Override
	protected String[] getAcceptedContentTypes() {
		// TODO get all media types from Rio?
		return ACCEPTED;
	}

}
