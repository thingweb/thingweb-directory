package de.thingweb.directory.servlet;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.Test;

import de.thingweb.directory.BaseTest;
import de.thingweb.directory.servlet.utils.MockHttpServletRequest;
import de.thingweb.directory.servlet.utils.MockHttpServletResponse;

public class RDFDocumentServletTest extends BaseTest {
	
	@Test
	public void testDoGetWithContentNegotiation() throws Exception {
		RDFDocumentServlet servlet = new RDFDocumentServlet();
		
		byte[] b = loadResource("samples/fanTD.jsonld");
		MockHttpServletRequest req = new MockHttpServletRequest("/", b, "application/ld+json");
		MockHttpServletResponse resp = new MockHttpServletResponse();
		
		String id = servlet.doAdd(req, resp);
		
		Map<String, String> headers = new HashMap<>();
		headers.put("Accept", "text/turtle");
		req = new MockHttpServletRequest("/" + id, new byte [0], "text/plain", headers, new HashMap<>());
		resp = new MockHttpServletResponse();
		
		servlet.doGet(req, resp);
		
		ByteArrayInputStream in = new ByteArrayInputStream(resp.getBytes());
		Model m = Rio.parse(in, BaseTest.BASE_URI, RDFFormat.TURTLE);
		assertFalse("RDF document could not be serialized (Turtle)", m.isEmpty());
	}
	
	@Test
	public void testDoPut() throws Exception {
		RDFDocumentServlet servlet = new RDFDocumentServlet();
		
		byte[] b = loadResource("samples/fanTD.jsonld");
		MockHttpServletRequest req = new MockHttpServletRequest("/", b, "application/ld+json");
		MockHttpServletResponse resp = new MockHttpServletResponse();
		
		String id = servlet.doAdd(req, resp);
		
		ByteArrayInputStream in = new ByteArrayInputStream(b);
		Model original = Rio.parse(in, BaseTest.BASE_URI, RDFFormat.JSONLD);
		
		b = loadResource("samples/fanTD_update.jsonld");
		req = new MockHttpServletRequest("/" + id, b, "application/ld+json");
		resp = new MockHttpServletResponse();
		
		servlet.doPut(req, resp);

		req = new MockHttpServletRequest("/" + id);
		resp = new MockHttpServletResponse();
		
		servlet.doGet(req, resp);
		
		in = new ByteArrayInputStream(resp.getBytes());
		Model updated = Rio.parse(in, BaseTest.BASE_URI, RDFFormat.JSONLD);
		assertFalse("Update on RDF document was not performed", Models.isomorphic(original, updated));
	}
	
	@Test
	public void testDoDelete() throws Exception {
		RDFDocumentServlet servlet = new RDFDocumentServlet();
		
		byte[] b = loadResource("samples/fanTD.jsonld");
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
		
		byte[] b = loadResource("samples/fanTD.jsonld");
		ByteArrayInputStream in = new ByteArrayInputStream(b);
		Model m = Rio.parse(in, BaseTest.BASE_URI, RDFFormat.JSONLD);

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
	public void testDoGetTimeout() throws ServletException, IOException, InterruptedException {
		RegistrationResourceServlet servlet = new RDFDocumentServlet();

		Map<String, String> params = new HashMap<>();
		params.put("lt", "1"); // 1s timeout
		MockHttpServletRequest req = new MockHttpServletRequest("/", new byte [0], "text/plain", new HashMap<>(), params);
		MockHttpServletResponse resp = new MockHttpServletResponse();
		
		String id = servlet.doAdd(req, resp);
		
		Thread.sleep(1500); // 1.5s sleep time (> resource timeout)
		
		req = new MockHttpServletRequest("/rd/" + id, new byte [0], "text/plain");
		resp = new MockHttpServletResponse();
		
		servlet.doGet(req, resp);
		
		assertEquals("Registered resource was not deleted after timeout", 404, resp.getStatus());
	}
	
	@Test
	public void testDoPutTimeout() throws Exception {
		RegistrationResourceServlet servlet = new RDFDocumentServlet();

		Map<String, String> params = new HashMap<>();
		params.put("lt", "1"); // 1s timeout
		MockHttpServletRequest req = new MockHttpServletRequest("/", new byte [0], "text/plain", new HashMap<>(), params);
		MockHttpServletResponse resp = new MockHttpServletResponse();
		
		String id = servlet.doAdd(req, resp);
		
		Thread.sleep(500); // 0.5s sleep time (< resource timeout)
		
		req = new MockHttpServletRequest("/td/" + id, new byte [0], "text/plain", new HashMap<>(), params);
		resp = new MockHttpServletResponse();
		
		servlet.doPut(req, resp);

		Thread.sleep(750); // 1.25s sleep time in total (> initial resource timeout)
		
		req = new MockHttpServletRequest("/td/" + id, new byte [0], "text/plain");
		resp = new MockHttpServletResponse();
		
		servlet.doGet(req, resp);
		
		assertEquals("Registered resource lifetime was not updated", 200, resp.getStatus());
	}

}
