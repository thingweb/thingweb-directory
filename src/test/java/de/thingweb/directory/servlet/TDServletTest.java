package de.thingweb.directory.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
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
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.junit.Ignore;
import org.junit.Test;

import com.github.jsonldjava.utils.JsonUtils;

import de.thingweb.directory.BaseTest;
import de.thingweb.directory.rest.CollectionItemServlet;
import de.thingweb.directory.rest.CollectionServlet;
import de.thingweb.directory.servlet.utils.MockHttpServletRequest;
import de.thingweb.directory.servlet.utils.MockHttpServletResponse;

public class TDServletTest extends BaseTest {
	
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
	
	@Test
	public void testTDServlet() throws Exception {
		MockTDServlet servlet = new MockTDServlet();
		
		byte[] b = loadResource("samples/fanTD.jsonld");
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
		MockTDServlet servlet = new MockTDServlet();
		
		InputStream td = cl.getResourceAsStream("samples/fanTD+temperatureSensorTD.jsonld");
		Model m = Rio.parse(td, BaseTest.BASE_URI, RDFFormat.JSONLD);
		servlet.generateItemID(m);

		assertEquals("Two child resources should have been created from input", 2, servlet.getAllItems().size());
	}

	@Test
	public void testWriteContent() throws IOException {
		MockTDServlet servlet = new MockTDServlet();
		
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
	
	@Test
	public void testDoDuplicatePost() throws Exception {
		MockTDServlet servlet = new MockTDServlet();
		MockCollectionServlet collServlet = new MockCollectionServlet(servlet);
		
		byte[] b = loadResource("samples/fanTD.jsonld");
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

}
