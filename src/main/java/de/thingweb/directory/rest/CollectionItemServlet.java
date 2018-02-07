package de.thingweb.directory.rest;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.thingweb.directory.ThingDirectory;

/**
 * 
 *  Assumes it maps to a path of the form {@code collection/*}.
 *
 * @author Victor Charpenay
 * @creation 06.02.2018
 *
 */
public abstract class CollectionItemServlet extends RESTServlet {

	protected Collection<String> items = new HashSet<>();
	
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		items.remove(getItemID(req));
	}
	
	/**
	 * Should return the name of the resource newly created
	 * 
	 * @param req
	 * @param resp
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	protected String doAdd(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		return generateItemID();
	}
	
	public Collection<String> getAllItems() {
		return items;
	}
	
	protected String generateItemID() {
		String id = UUID.randomUUID().toString();
		return id.substring(0, id.indexOf('-'));
	}
	
	protected String getItemID(HttpServletRequest req) {
		String uri = req.getRequestURI();
		return uri.substring(uri.lastIndexOf("/") + 1, uri.length());
	}
	
}
