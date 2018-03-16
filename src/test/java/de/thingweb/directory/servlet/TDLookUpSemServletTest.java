package de.thingweb.directory.servlet;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Ignore;
import org.junit.Test;

import com.github.jsonldjava.utils.JsonUtils;

import de.thingweb.directory.BaseTest;
import de.thingweb.directory.rest.CollectionItemServlet;
import de.thingweb.directory.rest.CollectionServlet;
import de.thingweb.directory.servlet.utils.MockHttpServletRequest;
import de.thingweb.directory.servlet.utils.MockHttpServletResponse;

public class TDLookUpSemServletTest extends BaseTest {
	
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

	@Test
	public void testDoGetWithQuery() throws Exception {
		TDServlet servlet = new TDServlet();
		MockCollectionServlet collServlet = new MockCollectionServlet(servlet);
		TDLookUpSemServlet lookUpServlet = new TDLookUpSemServlet(servlet);
		
		byte[] b = loadResource("samples/fanTD.jsonld");
		MockHttpServletRequest req = new MockHttpServletRequest("/", b, "application/ld+json");
		MockHttpServletResponse resp = new MockHttpServletResponse();

		collServlet.doPost(req, resp);
		
		b = loadResource("samples/temperatureSensorTD.jsonld");
		req = new MockHttpServletRequest("/", b, "application/ld+json");
		resp = new MockHttpServletResponse();

		collServlet.doPost(req, resp);
		String id = resp.getHeader("Location");

		String q = "?thing a <http://uri.etsi.org/m2m/saref#Sensor> .\n"
				+ "FILTER NOT EXISTS {"
				+ "  ?thing <http://www.w3.org/ns/td#providesInteractionPattern> ?i .\n"
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
