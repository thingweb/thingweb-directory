package de.thingweb.directory.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.thingweb.directory.ThingDirectory;

public class CollectionServlet extends RESTServlet {
	
	protected static final String LOCATION_HEADER = "Location";
	
	private static final String[] ACCEPTED = { "application/json" };
	
	protected final CollectionItemServlet itemServlet;
	
	public CollectionServlet(CollectionItemServlet child) {
		itemServlet = child;
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		super.doGet(req, resp);
		
		OutputStream out = resp.getOutputStream();
		try {
			out.write('[');
			
			Iterator<String> it = itemServlet.getAllItems().iterator();
			while (it.hasNext()) {
				out.write('"');
				out.write(it.next().getBytes());
				out.write('"');
				if (it.hasNext()) {
					out.write(',');
				}
			}
			
			out.write(']');
		} catch (IOException e) {
			ThingDirectory.LOG.error("Cannot write byte array", e);
			resp.sendError(500, e.getMessage()); // Internal Server Error
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String id = itemServlet.doAdd(req, resp);
		Collection<String> items = itemServlet.getAllItems();
		
		if (!items.contains(id)) {
			items.add(id);
		} else {
			ThingDirectory.LOG.info("Item already registered: " + id);
		}
		
		// TODO change status code if item already exists
		resp.setHeader(LOCATION_HEADER, id);
		resp.setStatus(201); // Created
	}
	
	@Override
	protected String[] getAcceptedContentTypes() {
		return ACCEPTED;
	}

}
