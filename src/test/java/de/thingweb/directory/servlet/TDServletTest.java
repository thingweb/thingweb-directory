package de.thingweb.directory.servlet;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

import org.eclipse.jetty.server.Request;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.junit.Ignore;
import org.junit.Test;

import com.github.jsonldjava.utils.JsonUtils;

import de.thingweb.directory.BaseTest;
import de.thingweb.directory.rest.RESTResource;
import de.thingweb.directory.servlet.utils.MockHttpServletRequest;
import de.thingweb.directory.servlet.utils.MockHttpServletResponse;

public class TDServletTest extends BaseTest {
	
	@Test
	public void testTDServlet() throws Exception {
		TDServlet servlet = new TDServlet();
		
		byte[] b = loadResource("samples/fanTD.jsonld");
		MockHttpServletRequest req = new MockHttpServletRequest("/td", b, "application/ld+json");
		MockHttpServletResponse resp = new MockHttpServletResponse();

		String id = servlet.doAdd(req, resp);
		assertNotNull("TD ID should have been returned", id);
		
		servlet = new TDServlet();
		assertEquals("TD should have been re-registered after recreation", 1, servlet.getAllItems().size()); 
	}

	@Test
	public void testGenerateItemID() throws RDFParseException, UnsupportedRDFormatException, IOException, ServletException {
		TDServlet servlet = new TDServlet();
		
		InputStream td = cl.getResourceAsStream("samples/fanTD.jsonld");
		Model m = Rio.parse(td, BaseTest.BASE_URI, RDFFormat.JSONLD);
		String id = servlet.generateItemID(m);
		
		assertEquals("Child resource name should be the TD @id", id, "urn%3AFan");

		td = cl.getResourceAsStream("samples/temperatureSensorTD.jsonld");
		m = Rio.parse(td, BaseTest.BASE_URI, RDFFormat.JSONLD);
		id = servlet.generateItemID(m);
		assertTrue("Not all TDs were registered (simultaneous registration)", id.matches("[0123456789abcdef]{8}"));
	}
	
	@Test
	@Ignore
	public void testGenerateMultipleItemID() throws Exception {
		TDServlet servlet = new TDServlet();
		
		InputStream td = cl.getResourceAsStream("samples/fanTD+temperatureSensorTD.jsonld");
		Model m = Rio.parse(td, BaseTest.BASE_URI, RDFFormat.JSONLD);
		servlet.generateItemID(m);
		
		assertEquals("Two child resources should have been created from input", 2, servlet.getAllItems().size());
	}

	@Test
	public void testWriteContent() throws IOException {
		TDServlet servlet = new TDServlet();
		
		byte[] b = loadResource("samples/fanTD.jsonld");
		Map<String, String> headers = new HashMap<>();
		headers.put("Accept", "application/ld+json");
		MockHttpServletRequest req = new MockHttpServletRequest("/td", b, "application/ld+json", headers);
		MockHttpServletResponse resp = new MockHttpServletResponse();
		
		Model m = Rio.parse(new ByteArrayInputStream(b), BaseTest.BASE_URI, RDFFormat.JSONLD);
		
		servlet.writeContent(m, req, resp);
		
		b = resp.getBytes();
		Object td = JsonUtils.fromString(new String(b));
		assertTrue("TD should be a (framed) JSON object", td instanceof Map);
	}

}
