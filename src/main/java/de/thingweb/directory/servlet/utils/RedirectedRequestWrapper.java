package de.thingweb.directory.servlet.utils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * 
 * Wraps the input request, up to the requested path.
 *
 * @author Victor Charpenay
 * @creation 07.02.2018
 *
 */
public class RedirectedRequestWrapper extends HttpServletRequestWrapper {

	private String path;
	
	public RedirectedRequestWrapper(HttpServletRequest req, String redirectedTo) {
		super(req);
		path = redirectedTo;
	}
	
	@Override
	public String getRequestURI() {
		return path;
	}
	
}
