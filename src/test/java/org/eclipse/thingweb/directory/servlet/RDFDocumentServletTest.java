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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.thingweb.directory.ServletTestSuite;
import org.eclipse.thingweb.directory.rest.CollectionItemServlet;
import org.eclipse.thingweb.directory.rest.CollectionServlet;
import org.eclipse.thingweb.directory.servlet.utils.MockHttpServletRequest;
import org.eclipse.thingweb.directory.servlet.utils.MockHttpServletResponse;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class RDFDocumentServletTest {
	
	private static class MockRDFDocumentServlet extends RDFDocumentServlet {
		
		@Override
		protected String generateItemID() {
			// only used for proper testing of protected method
			return super.generateItemID();
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
	public void testDoGetWithContentNegotiation() throws Exception {
		RDFDocumentServlet servlet = new RDFDocumentServlet();
		
		byte[] b = ServletTestSuite.loadResource("samples/fanTD.jsonld");
		MockHttpServletRequest req = new MockHttpServletRequest("/", b, "application/ld+json");
		MockHttpServletResponse resp = new MockHttpServletResponse();
		
		String id = servlet.doAdd(req, resp);
		
		Map<String, String> headers = new HashMap<>();
		headers.put("Accept", "text/turtle");
		req = new MockHttpServletRequest("/" + id, new byte [0], "text/plain", headers, new HashMap<>());
		resp = new MockHttpServletResponse();
		
		servlet.doGet(req, resp);
		
		ByteArrayInputStream in = new ByteArrayInputStream(resp.getBytes());
		Model m = Rio.parse(in, ServletTestSuite.BASE_URI, RDFFormat.TURTLE);
		assertFalse("RDF document could not be serialized (Turtle)", m.isEmpty());
	}
	
	@Test
	public void testDoPut() throws Exception {
		RDFDocumentServlet servlet = new RDFDocumentServlet();
		
		byte[] b = ServletTestSuite.loadResource("samples/fanTD.jsonld");
		MockHttpServletRequest req = new MockHttpServletRequest("/", b, "application/ld+json");
		MockHttpServletResponse resp = new MockHttpServletResponse();
		
		String id = servlet.doAdd(req, resp);
		
		ByteArrayInputStream in = new ByteArrayInputStream(b);
		Model original = Rio.parse(in, ServletTestSuite.BASE_URI, RDFFormat.JSONLD);
		
		b = ServletTestSuite.loadResource("samples/fanTD_update.jsonld");
		req = new MockHttpServletRequest("/" + id, b, "application/ld+json");
		resp = new MockHttpServletResponse();
		
		servlet.doPut(req, resp);

		req = new MockHttpServletRequest("/" + id);
		resp = new MockHttpServletResponse();
		
		servlet.doGet(req, resp);
		
		in = new ByteArrayInputStream(resp.getBytes());
		Model updated = Rio.parse(in, ServletTestSuite.BASE_URI, RDFFormat.JSONLD);
		assertFalse("Update on RDF document was not performed", Models.isomorphic(original, updated));
	}
	
	@Test
	public void testDoDelete() throws Exception {
		RDFDocumentServlet servlet = new RDFDocumentServlet();
		
		byte[] b = ServletTestSuite.loadResource("samples/fanTD.jsonld");
		MockHttpServletRequest req = new MockHttpServletRequest("/", b, "application/ld+json");
		MockHttpServletResponse resp = new MockHttpServletResponse();
		
		String id = servlet.doAdd(req, resp);
		
		req = new MockHttpServletRequest("/" + id);
		resp = new MockHttpServletResponse();
		
		servlet.doDelete(req, resp);
		
		assertEquals("RDF document deletion returned success code", 200, resp.getStatus());
		
		req = new MockHttpServletRequest("/" + id);
		resp = new MockHttpServletResponse();
		
		servlet.doGet(req, resp);
		
		assertEquals("RDF document was not deleted", 404, resp.getStatus());
	}
	
	@Test
	public void testDoAdd() throws Exception {
		RDFDocumentServlet servlet = new RDFDocumentServlet();
		
		byte[] b = ServletTestSuite.loadResource("samples/fanTD.jsonld");
		ByteArrayInputStream in = new ByteArrayInputStream(b);
		Model m = Rio.parse(in, ServletTestSuite.BASE_URI, RDFFormat.JSONLD);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Rio.write(m, out, RDFFormat.TURTLE);
		b = out.toByteArray();
		MockHttpServletRequest req = new MockHttpServletRequest("/", b, "text/turtle");
		MockHttpServletResponse resp = new MockHttpServletResponse();
		
		String id = servlet.doAdd(req, resp);

		req = new MockHttpServletRequest("/" + id);
		resp = new MockHttpServletResponse();
		
		servlet.doGet(req, resp);
		
		assertEquals("RDF document could not be parsed (Turtle)", 200, resp.getStatus());
	}
	
	@Test
	public void testGenerateItemID() throws Exception {
		MockRDFDocumentServlet servlet = new MockRDFDocumentServlet();
		
		String id = servlet.generateItemID();
		assertTrue("RDF document ID is not a URI (as per RFC 3986)", id.matches("^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?"));
	}

	@Test
	@Ignore
	public void testDoGetTimeout() throws ServletException, IOException, InterruptedException {
		RegistrationResourceServlet servlet = new RDFDocumentServlet();
		MockCollectionServlet collServlet = new MockCollectionServlet(servlet);

		Map<String, String> params = new HashMap<>();
		params.put("lt", "1"); // 1s timeout
		MockHttpServletRequest req = new MockHttpServletRequest("/", new byte [0], "text/plain", new HashMap<>(), params);
		MockHttpServletResponse resp = new MockHttpServletResponse();
		
		collServlet.doPost(req, resp);
		String id = resp.getHeader("Location");
		
		Thread.sleep(500); // 0.5s sleep time (< resource timeout)
		
		req = new MockHttpServletRequest("/rd/" + id, new byte [0], "text/plain");
		resp = new MockHttpServletResponse();
		
		servlet.doGet(req, resp);

		assertNotEquals("Registered resource was deleted before timeout", 404, resp.getStatus());
		
		Thread.sleep(1000); // 1.5s sleep time (> resource timeout)
		
		servlet.doGet(req, resp);
		
		assertEquals("Registered resource was not deleted after timeout", 404, resp.getStatus());
	}
	
	@Test
	@Ignore
	public void testDoPutTimeout() throws Exception {
		RegistrationResourceServlet servlet = new RDFDocumentServlet();
		MockCollectionServlet collServlet = new MockCollectionServlet(servlet);

		Map<String, String> params = new HashMap<>();
		params.put("lt", "1"); // 1s timeout
		MockHttpServletRequest req = new MockHttpServletRequest("/", new byte [0], "text/plain", new HashMap<>(), params);
		MockHttpServletResponse resp = new MockHttpServletResponse();
		
		collServlet.doPost(req, resp);
		String id = resp.getHeader("Location");
		
		Thread.sleep(500); // 0.5s sleep time (< resource timeout)
		
		params.put("lt", "3600"); // 1h timeout (~ infinite)
		req = new MockHttpServletRequest("/td/" + id, new byte [0], "text/plain", new HashMap<>(), params);
		resp = new MockHttpServletResponse();
		
		servlet.doPut(req, resp);

		Thread.sleep(1000); // 1.5s sleep time in total (> initial resource timeout)
		
		req = new MockHttpServletRequest("/td/" + id, new byte [0], "text/plain");
		resp = new MockHttpServletResponse();
		
		servlet.doGet(req, resp);
		
		assertEquals("Registered resource lifetime was not updated", 200, resp.getStatus());
	}

}
