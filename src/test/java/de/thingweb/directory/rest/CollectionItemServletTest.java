package de.thingweb.directory.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;

import javax.servlet.ServletException;

import org.junit.Ignore;
import org.junit.Test;

import de.thingweb.directory.BaseTest;
import de.thingweb.directory.servlet.utils.MockHttpServletRequest;
import de.thingweb.directory.servlet.utils.MockHttpServletResponse;

public class CollectionItemServletTest extends BaseTest {

	private static class MockCollectionItemServlet extends CollectionItemServlet {
		
		private final static String[] ct = { "text/plain" };
		
		@Override
		protected String[] getAcceptedContentTypes() {
			return ct;
		}
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
	@Ignore
	public void testDoAdd() throws ServletException, IOException {
		CollectionItemServlet servlet = new MockCollectionItemServlet();
		
		assertEquals("Generic collection was not empty after creation", 0, servlet.getAllItems().size());
		
		MockHttpServletRequest req = new MockHttpServletRequest("/td");
		MockHttpServletResponse resp = new MockHttpServletResponse();

		String id = servlet.doAdd(req, resp);
		
		// FIXME items managed by collection servlet
		assertEquals("Collection did not add provided item", 1, servlet.getAllItems().size());
		assertEquals("Returned item ID was not as expected", servlet.getAllItems().iterator().next(), id);
	}

	@Test
	public void testGenerateItemID() {
		CollectionItemServlet servlet = new MockCollectionItemServlet();
		
		String id = servlet.generateItemID();
		String otherId = servlet.generateItemID();
		
		assertNotEquals("The collection items generated IDs collide", id, otherId);
	}

}
