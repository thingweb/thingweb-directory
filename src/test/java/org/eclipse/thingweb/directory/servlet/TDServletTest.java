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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.eclipse.thingweb.directory.ServletTestSuite;
import org.eclipse.thingweb.directory.ThingDirectory;
import org.eclipse.thingweb.directory.rest.CollectionItemServlet;
import org.eclipse.thingweb.directory.rest.CollectionServlet;
import org.eclipse.thingweb.directory.servlet.utils.MockHttpServletRequest;
import org.eclipse.thingweb.directory.servlet.utils.MockHttpServletResponse;
import org.eclipse.thingweb.directory.sparql.client.Connector;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.github.jsonldjava.utils.JsonUtils;

public class TDServletTest {
	
	private static class MockTDServlet extends TDServlet {
		
		@Override
		protected Collection<String> getAllItems() {
			// only used for proper testing of protected method
			return super.getAllItems();
		}
		
	}
	
	private static class MockCollectionServlet extends CollectionServlet {
		
		public MockCollectionServlet(CollectionItemServlet servlet) {
			super(servlet);
		}
		
		@Override
		protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			// only used for proper testing of protected method
			super.doPost(req, resp);
		}
		
		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			// only used for proper testing of protected method
			super.doGet(req, resp);
		}
		
	}
	
	@BeforeClass
	public static void setUpRDFStore() throws Exception {
		ServletTestSuite.setUpRDFStore();
	}

	@AfterClass
	public static void destroyRDFStore() throws Exception {
		ServletTestSuite.destroyRDFStore();
	}
	
	@Before
	public void cleanRDFStore() throws Exception {
		ServletTestSuite.cleanRDFStore();
	}
	
	@Test
	public void testTDServlet() throws Exception {
		MockTDServlet servlet = new MockTDServlet();
		
		byte[] b = ServletTestSuite.loadResource("samples/fanTD.jsonld");
		MockHttpServletRequest req = new MockHttpServletRequest("/td", b, "application/ld+json");
		MockHttpServletResponse resp = new MockHttpServletResponse();

		String id = servlet.doAdd(req, resp);
		assertNotNull("TD ID should have been returned", id);
		
		servlet = new MockTDServlet();
		assertEquals("TD should have been re-registered after recreation", 1, servlet.getAllItems().size()); 
	}

	@Test
	public void testGenerateItemID() throws RDFParseException, UnsupportedRDFormatException, IOException, ServletException {
		MockTDServlet servlet = new MockTDServlet();
		
		byte[] b = ServletTestSuite.loadResource("samples/fanTD.jsonld");
		MockHttpServletRequest req = new MockHttpServletRequest("/td", b, "application/ld+json");
		MockHttpServletResponse resp = new MockHttpServletResponse();
		Model m = servlet.readContent(req, resp);
		
		String id = servlet.generateItemID(m);
		
		assertEquals("Child resource name should be the TD @id", id, "urn:Fan");
	}
	
	@Test
	@Ignore
	public void testGenerateMultipleItemID() throws Exception {
		MockTDServlet servlet = new MockTDServlet();
		
		ClassLoader cl = this.getClass().getClassLoader();
		InputStream td = cl.getResourceAsStream("samples/fanTD+temperatureSensorTD.jsonld");
		Model m = Rio.parse(td, ServletTestSuite.BASE_URI, RDFFormat.JSONLD);
		servlet.generateItemID(m);

		assertEquals("Two child resources should have been created from input", 2, servlet.getAllItems().size());
	}
	
	@Test
	public void testReadContent() throws Exception {
		MockTDServlet servlet = new MockTDServlet();
		
		byte[] b = ServletTestSuite.loadResource("samples/fanTD.jsonld");
		MockHttpServletRequest req = new MockHttpServletRequest("/td", b, "application/ld+json");
		MockHttpServletResponse resp = new MockHttpServletResponse();
		Model m = servlet.readContent(req, resp);
		
		// TODO not only: should have all original statements
		assertNotNull("TD should have be parsed as JSON-LD 1.1", m);
	}

	@Test
	public void testWriteContent() throws IOException {
		MockTDServlet servlet = new MockTDServlet();
		
		byte[] b = ServletTestSuite.loadResource("samples/fanTD.jsonld");
		Map<String, String> headers = new HashMap<>();
		headers.put("Accept", "application/ld+json");
		MockHttpServletRequest req = new MockHttpServletRequest("/td", b, "application/ld+json", headers);
		MockHttpServletResponse resp = new MockHttpServletResponse();
		
		Model m = servlet.readContent(req, resp);
		servlet.writeContent(m, req, resp);
		
		b = resp.getBytes();
		Object td = JsonUtils.fromString(new String(b));
		assertTrue("TD should be a (framed) JSON object", td instanceof Map);
	}
	
	@Test
	public void testDoDuplicatePost() throws Exception {
		MockTDServlet servlet = new MockTDServlet();
		MockCollectionServlet collServlet = new MockCollectionServlet(servlet);
		
		byte[] b = ServletTestSuite.loadResource("samples/fanTD.jsonld");
		MockHttpServletRequest req = new MockHttpServletRequest("/", b, "application/ld+json");
		MockHttpServletResponse resp = new MockHttpServletResponse();

		collServlet.doPost(req, resp);
		String id = resp.getHeader("Location");

		req = new MockHttpServletRequest("/", b, "application/ld+json");
		resp = new MockHttpServletResponse();
		
		collServlet.doPost(req, resp);
		String otherId = resp.getHeader("Location");
		
		assertEquals("TD registered twice should not have been duplicated", id, otherId);

		req = new MockHttpServletRequest("/");
		resp = new MockHttpServletResponse();
		
		collServlet.doGet(req, resp);
		
		b = resp.getBytes();
		Object td = JsonUtils.fromString(new String(b));
		assertEquals("Duplicate TD was not detected", 1, ((List) td).size());
	}
	
	@Test
	public void testDoGet() throws Exception {
		String src = new String(ServletTestSuite.loadResource("td-schema.json"));
		Schema schema = SchemaLoader.load(new JSONObject(src));
		
		MockTDServlet servlet = new MockTDServlet();
		MockCollectionServlet collServlet = new MockCollectionServlet(servlet);
		
		byte[] b = ServletTestSuite.loadResource("samples/fanTD.jsonld");
		MockHttpServletRequest req = new MockHttpServletRequest("/td", b, "application/ld+json");
		MockHttpServletResponse resp = new MockHttpServletResponse();

		collServlet.doPost(req, resp);
		String id = resp.getHeader("Location");

		Map<String, String> headers = new HashMap<>();
		headers.put("Accept", "application/ld+json");
		req = new MockHttpServletRequest("/td/" + id, new byte [0], "text/plain", headers);
		resp = new MockHttpServletResponse();
		
		servlet.doGet(req, resp);
		
		JSONObject td = new JSONObject(new String(resp.getBytes()));
		schema.validate(td);
	}

}
