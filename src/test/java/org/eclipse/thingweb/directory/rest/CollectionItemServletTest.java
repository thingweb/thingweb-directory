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
package org.eclipse.thingweb.directory.rest;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.thingweb.directory.ServletTestSuite;
import org.eclipse.thingweb.directory.servlet.utils.MockHttpServletRequest;
import org.eclipse.thingweb.directory.servlet.utils.MockHttpServletResponse;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CollectionItemServletTest {

	static class MockCollectionItemServlet extends CollectionItemServlet {
		
		private static int counter = 0;
		
		private final static String[] ct = { "text/plain" };
		
		@Override
		protected String[] getAcceptedContentTypes() {
			return ct;
		}
		
		@Override
		protected String generateItemID() {
			return Integer.toString(counter++);
		}
		
		@Override
		protected String getItemID(HttpServletRequest req) {
			String uri = req.getRequestURI();
			String id = uri.substring(uri.lastIndexOf("/") + 1, uri.length());
			return id;
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
	public void testDoDelete() throws ServletException, IOException {
		CollectionItemServlet servlet = new MockCollectionItemServlet();
		
		MockHttpServletRequest req = new MockHttpServletRequest("/td");
		MockHttpServletResponse resp = new MockHttpServletResponse();

		String id = servlet.doAdd(req, resp);

		req = new MockHttpServletRequest("/td/" + id);
		servlet.doDelete(req, resp);
		
		assertEquals("Collection did not delete item", 0, servlet.getAllItems().size());
	}

	@Test
	public void testDoAdd() throws ServletException, IOException {
		CollectionItemServlet servlet = new MockCollectionItemServlet();
		
		assertEquals("Generic collection was not empty after creation", 0, servlet.getAllItems().size());
		
		MockHttpServletRequest req = new MockHttpServletRequest("/td");
		MockHttpServletResponse resp = new MockHttpServletResponse();

		String id = servlet.doAdd(req, resp);
		
		assertEquals("Collection did not add provided item", 1, servlet.getAllItems().size());
		assertEquals("Returned item ID was not as expected", servlet.getAllItems().iterator().next(), id);
	}

}
