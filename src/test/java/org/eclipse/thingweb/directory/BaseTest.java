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
package org.eclipse.thingweb.directory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.thingweb.directory.ThingDirectory;
import org.eclipse.thingweb.directory.sparql.client.Connector;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * configuration common to all unit tests
 */
public class BaseTest {
	
	protected static final String BASE_URI = "http://example.org";
	
	protected static ThingDirectory directory;
	
	protected final ClassLoader cl = ThingDirectory.get().getClass().getClassLoader();
	
	@BeforeClass
	public static void setUpRDFStore() throws Exception {
		// Note: ThingDirectory needs a proper connection to the RDF store to retrieve existing resources
		// Connector thus must be initialized before getting the ThingDirectory singleton
		Connector.init();
		directory = ThingDirectory.get();
	}

	@AfterClass
	public static void destroyRDFStore() throws Exception {
		// nothing to do
	}

	@Before
	public void cleanRDFStore() throws Exception {
		RepositoryConnection conn = Connector.getRepositoryConnection();
		conn.clear(); // delete all content in repository
	}

	protected byte[] loadResource(String location) throws IOException {
		InputStream in = cl.getResourceAsStream(location);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		byte[] buf = new byte[1024];
		int len;

		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();

		return out.toByteArray();
	}

}
