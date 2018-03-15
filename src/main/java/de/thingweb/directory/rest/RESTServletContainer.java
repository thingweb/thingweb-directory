package de.thingweb.directory.rest;

/**
 * 
 * A Servlet container can add REST resources and collections.
 *
 * @author Victor Charpenay
 * @creation 06.02.2018
 *
 */
public abstract class RESTServletContainer {
	
	public void addServletWithMapping(String path, RESTServlet servlet) {
		if (servlet instanceof CollectionServlet) {
			addServletWithMapping(path + "/*", ((CollectionServlet) servlet).getItemServlet());
		}
	}
	
	public abstract void start();
	
	public abstract void stop();
	
	public abstract void join();
  
}
