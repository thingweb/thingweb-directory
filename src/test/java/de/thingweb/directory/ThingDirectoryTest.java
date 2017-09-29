package de.thingweb.directory;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.codehaus.plexus.util.StringOutputStream;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import de.thingweb.directory.ThingDirectory;
import de.thingweb.directory.coap.CoAPServer;
import de.thingweb.directory.http.HTTPServer;
import de.thingweb.directory.rest.RESTServerInstance;
import de.thingweb.directory.sparql.client.Connector;

public class ThingDirectoryTest extends BaseTest {
	
	public static final int COAP_PORT = 5684;
	public static final int HTTP_PORT = 8081;
	
	private static Thread thread;
	
	@BeforeClass
	public static void runRepository() throws Exception {
		ThingDirectory directory = ThingDirectory.get();

		Set<RESTServerInstance> servers = new HashSet<>();
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
		assertEquals("CoAP server instance is not reachable", ResponseCode.VALID, coapResponse.getCode());
		
		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpGet httpRequest = new HttpGet("http://localhost:" + HTTP_PORT + "/");
		HttpResponse httpResponse = httpClient.execute(httpRequest);
		assertEquals("HTTP server instance is not reachable", 200, httpResponse.getStatusLine().getStatusCode());
	}
	
	@Test
	@Ignore
	public void testServerSynchronicity() throws Exception {
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
