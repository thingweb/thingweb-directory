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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.eclipse.thingweb.directory.ThingDirectory;
import org.eclipse.thingweb.directory.servlet.exception.MalformedDocumentException;
import org.eclipse.thingweb.directory.sparql.client.Queries;
import org.eclipse.thingweb.directory.utils.TDTransform;
import org.eclipse.thingweb.directory.vocabulary.TD;

import com.fasterxml.jackson.core.JsonParseException;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

public class TDServlet extends RDFDocumentServlet {
	
	protected static final String TD_CONTEXT_URI = "https://w3c.github.io/wot/w3c-wot-td-context.jsonld";
	
	@Override
	protected Collection<String> getAllItems() {
		Collection<String> ids = new HashSet<String>();
		
		try {
			String pattern = String.format("?td a <%s>", TD.Thing.stringValue());
			
			// fetches items IDs from RDF store if accessible
			try (TupleQueryResult res = Queries.listResources(pattern)) {
				while (res.hasNext()) {
					String uri = res.next().getValue("res").stringValue();
					String id = getItemId(uri);
					
					ids.add(id);
				}
				
				items = ids;
			}
		} catch (Exception e) {
			ThingDirectory.LOG.error("Cannot fetch existing TDs from the RDF store", e);
		}

		return items;
	}
	
	@Override
	protected Model readContent(HttpServletRequest req, HttpServletResponse resp) throws RDFParseException, UnsupportedRDFormatException, IOException {
		RDFFormat format = getContentFormat(req);
		
		if (format.equals(RDFFormat.JSONLD)) {
			TDTransform transform = new TDTransform(req.getInputStream());
			String td = transform.asJsonLd10();
			return Rio.parse(new ByteArrayInputStream(td.getBytes()), getBaseURI(req), format);
		}
		
		return super.readContent(req, resp);
	}
	
	@Override
	protected void writeContent(Model m, HttpServletRequest req, HttpServletResponse resp) throws RDFHandlerException, IOException {		
		RDFFormat format = getAcceptedFormat(req);
		
		if (format.equals(RDFFormat.JSONLD)) {
			// performs JSON-LD framing
			try {
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				Rio.write(m, buffer, format);
				
				JsonLdOptions opts = new JsonLdOptions();
				opts.setPruneBlankNodeIdentifiers(true);
				opts.setUseNativeTypes(true);
				opts.setCompactArrays(true);
				
				Object obj = JsonUtils.fromString(buffer.toString());
				Object frame = getFrameWithBase(getBaseURI(req));
				Object framed = JsonLdProcessor.frame(obj, frame, opts);
				
				resp.setContentType(RDFFormat.JSONLD.getDefaultMIMEType());
				
				TDTransform transform = new TDTransform(JsonUtils.toString(framed).getBytes());
				String td = transform.asJsonLd11();
				resp.getOutputStream().write(td.getBytes());
			} catch (JsonLdError e) {
				ThingDirectory.LOG.error("Could not frame TD output (JSON-LD)", e);
				resp.sendError(500); // Internal Server Error
			}
		} else {
			super.writeContent(m, req, resp);
		}
	}

	@Override
	protected String generateItemID(Model m) throws MalformedDocumentException {
		Set<Resource> things = m.filter(null, RDF.TYPE, TD.Thing).subjects();
		
		Iterator<Resource> iterator = things.iterator();
		if (!iterator.hasNext()) {
			throw new MalformedDocumentException("No instance of td:Thing found in the RDF payload");
		}
		
		Resource res = iterator.next();
		String id = res instanceof IRI ? URLEncoder.encode(res.toString()) : super.generateItemID();
		
		if (iterator.hasNext()) {
			// TODO should split TD documents
		}

		return id;
	}
	
	protected Object getFrameWithBase(String base) throws JsonParseException, IOException {
		// TODO do changes in Groovy instead?		
		return JsonUtils.fromString("{\"@context\":"
				+ "[\"" + TD_CONTEXT_URI + "\","
				+ "{\"@base\":\"" + base + "\"}],"
				+ "\"@type\": \"Thing\"}");
	}
	
	/**
	 * breadth-first traversal of the RDF model
	 * 
	 * @param root starting point of the traversal
	 * @param visited set of visited nodes (should be empty)
	 * @return
	 */
	private static Model extractTD(Model m, Resource root, Set<Resource> visited) {
		Model td = new ModelBuilder().build();
		
		visited.add(root);
		
		m.filter(root, null, null).forEach(stm -> {
			IRI p = stm.getPredicate();
			Value o = stm.getObject();
			td.add(stm);
			if (!p.equals(RDF.TYPE) && o instanceof Resource) {
				Resource node = (Resource) o;
				if (!m.contains(node, RDF.TYPE, SimpleValueFactory.getInstance().createIRI(TD.Thing.stringValue())) && !visited.contains(node)) {
					Model submodel = extractTD(m, node, visited);
					submodel.forEach(substm -> {
						td.add(substm);
					});
				}
			}
		});
		
		return td;
	}
	
}
