package de.thingweb.directory.rest;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;

import org.junit.Test;

import com.github.jsonldjava.utils.JsonUtils;

import de.thingweb.directory.BaseTest;
import de.thingweb.directory.rest.CollectionItemServlet;
import de.thingweb.directory.rest.CollectionServlet;
import de.thingweb.directory.servlet.utils.MockHttpServletRequest;
import de.thingweb.directory.servlet.utils.MockHttpServletResponse;

public class CollectionServletTest extends BaseTest {
	
	private static class MockCollectionItemServlet extends CollectionItemServlet {
		
		private final String[] ct = { "text/plain" };
		
		@Override
		protected String[] getAcceptedContentTypes() {
			return ct;
		}
		
	}

	@Test
	public void testDoPost() throws ServletException, IOException {
		CollectionServlet servlet = new CollectionServlet(new MockCollectionItemServlet());
		
		MockHttpServletRequest req = new MockHttpServletRequest("/");
		MockHttpServletResponse resp = new MockHttpServletResponse();

		servlet.doPost(req, resp);
		
		req = new MockHttpServletRequest("/");
		resp = new MockHttpServletResponse();

		servlet.doPost(req, resp);
		
		req = new MockHttpServletRequest("/");
		resp = new MockHttpServletResponse();

		servlet.doGet(req, resp);
		
		byte[] b = resp.getBytes();
		Object o = JsonUtils.fromString(new String(b));
		assertTrue("Unexpected JSON representation (generic resource collection)", o instanceof List);
		assertEquals("The collection does not have the expected number of items", 2, ((List) o).size());
	}

}
