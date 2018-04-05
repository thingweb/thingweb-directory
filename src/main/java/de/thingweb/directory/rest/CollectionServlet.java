package de.thingweb.directory.rest;

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
	
	public static final String LOCATION_HEADER = "Location";
	
	private static final String[] ACCEPTED = { "application/json" };
	
	protected final CollectionItemServlet itemServlet;
	
	public CollectionServlet(CollectionItemServlet child) {
		itemServlet = child;
	}
	
	public CollectionItemServlet getItemServlet() {
		return itemServlet;
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
		int delta = itemServlet.getAllItems().size();
		String id = itemServlet.doAdd(req, resp);
		delta = itemServlet.getAllItems().size() - delta;
		
		if (delta > 0) {
			resp.setStatus(201); // Created
		} else {
			ThingDirectory.LOG.info("Item already registered: " + id);
			resp.setStatus(204); // No Content
		}
		
		resp.setHeader(LOCATION_HEADER, id);
	}
	
	@Override
	protected String[] getAcceptedContentTypes() {
		return ACCEPTED;
	}

}
