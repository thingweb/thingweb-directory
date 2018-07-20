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

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.thingweb.directory.ThingDirectory;
import org.eclipse.thingweb.directory.coap.CoAPServer;
import org.eclipse.thingweb.directory.http.HTTPServer;
import org.eclipse.thingweb.directory.rest.RESTServletContainer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class ThingDirectoryTest {
	
	public static final int COAP_PORT = 5684;
	public static final int HTTP_PORT = 8081;
	
	private static Thread thread;
	
	@BeforeClass
	public static void runRepository() throws Exception {
		ThingDirectory directory = ThingDirectory.get();

		Set<RESTServletContainer> servers = new HashSet<>();
	    servers.add(new CoAPServer(COAP_PORT));
	    servers.add(new HTTPServer(HTTP_PORT));

	    thread = new Thread(() -> {
		    directory.run(servers);
	    });
	    thread.run();
	}

	@AfterClass
	public static void terminateRepository() throws Exception {
		// TODO notify directory to terminate instead
//		thread.destroy();
	}
	
	@Test
	public void testSetup() throws Exception {
		CoapClient coapClient = new CoapClient("coap://localhost:" + COAP_PORT + "/");
		CoapResponse coapResponse = coapClient.get();
		assertEquals("CoAP server instance is not reachable", ResponseCode.CONTENT, coapResponse.getCode());
		
		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpGet httpRequest = new HttpGet("http://localhost:" + HTTP_PORT + "/");
		HttpResponse httpResponse = httpClient.execute(httpRequest);
		assertEquals("HTTP server instance is not reachable", 200, httpResponse.getStatusLine().getStatusCode());
	}
	
	@Test
	@Ignore
	public void testServerSynchronicity() throws Exception {
		ClassLoader cl = this.getClass().getClassLoader();
		InputStream in = cl.getResourceAsStream("samples/fanTD.jsonld");
		
		StringBuilder builder = new StringBuilder();
		byte[] buf = new byte [2048];
		int read = 0;
		while ((read = in.read(buf)) >= 0) {
			builder.append(new String(buf, 0, read));
		}
		String payload = builder.toString();
		
		CoapClient coapClient = new CoapClient("coap://localhost:" + COAP_PORT + "/td");
		CoapResponse coapResponse = coapClient.post(payload, MediaTypeRegistry.APPLICATION_JSON);
		String path = coapResponse.getOptions().getLocationPathString();
		
		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpUriRequest httpRequest = new HttpDelete("http://localhost:" + HTTP_PORT + path);
		HttpResponse httpResponse = httpClient.execute(httpRequest);
		assertEquals("HTTP server instance not in sync with CoAP server instance", 200, httpResponse.getStatusLine().getStatusCode());

		httpRequest = new HttpPost("http://localhost:" + HTTP_PORT + "/td");
		((HttpPost) httpRequest).setEntity(new StringEntity(payload));
		httpResponse = httpClient.execute(httpRequest);
		path = httpResponse.getFirstHeader("Location").getValue();
		
		coapClient = new CoapClient("coap://localhost:" + COAP_PORT + path);
		coapResponse = coapClient.delete();
		assertEquals("CoAP server instance not in sync with HTTP server instance", ResponseCode.DELETED, coapResponse.getCode()); 
	}

}
