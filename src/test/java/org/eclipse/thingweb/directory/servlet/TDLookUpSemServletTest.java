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

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.thingweb.directory.BaseTest;
import org.eclipse.thingweb.directory.ServletTestSuite;
import org.eclipse.thingweb.directory.rest.CollectionItemServlet;
import org.eclipse.thingweb.directory.rest.CollectionServlet;
import org.eclipse.thingweb.directory.servlet.TDLookUpSemServlet;
import org.eclipse.thingweb.directory.servlet.TDServlet;
import org.eclipse.thingweb.directory.servlet.utils.MockHttpServletRequest;
import org.eclipse.thingweb.directory.servlet.utils.MockHttpServletResponse;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.github.jsonldjava.utils.JsonUtils;

public class TDLookUpSemServletTest {
	
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
	public void testDoGetWithQuery() throws Exception {
		TDServlet servlet = new TDServlet();
		MockCollectionServlet collServlet = new MockCollectionServlet(servlet);
		TDLookUpSemServlet lookUpServlet = new TDLookUpSemServlet(servlet);
		
		byte[] b = ServletTestSuite.loadResource("samples/fanTD.jsonld");
		MockHttpServletRequest req = new MockHttpServletRequest("/", b, "application/ld+json");
		MockHttpServletResponse resp = new MockHttpServletResponse();

		collServlet.doPost(req, resp);
		
		b = ServletTestSuite.loadResource("samples/temperatureSensorTD.jsonld");
		req = new MockHttpServletRequest("/", b, "application/ld+json");
		resp = new MockHttpServletResponse();

		collServlet.doPost(req, resp);
		String id = resp.getHeader("Location");

		String q = "?thing a <http://uri.etsi.org/m2m/saref#Sensor> .\n"
				+ "FILTER NOT EXISTS {"
				+ "  ?thing <http://www.w3.org/ns/td#actions> ?i .\n"
				+ "  ?i a <http://uri.etsi.org/m2m/saref#ToggleCommand> .\n"
				+ "}";
		HashMap<String, String> params = new HashMap<>();
		params.put("query", q);
		req = new MockHttpServletRequest("/", new byte [0], "text/plain", new HashMap<>(), params);
		resp = new MockHttpServletResponse();
		
		lookUpServlet.doGet(req, resp);
		
		Object o = JsonUtils.fromString(new String(resp.getBytes()));
		assertTrue("Lookup result is not formatted as expected", o instanceof Map);
		assertEquals("SPARQL filter was not applied", 1, ((Map) o).keySet().size());
		assertEquals("SPARQL filter was not applied correctly", id, ((Map) o).keySet().iterator().next());
	}

}
