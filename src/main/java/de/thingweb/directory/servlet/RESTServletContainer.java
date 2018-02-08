package de.thingweb.directory.servlet;

/**
 * 
 * A Servlet container can add REST resources and collections.
 *
 * @author Victor Charpenay
 * @creation 06.02.2018
 *
 */
public interface RESTServletContainer {
	
	public void addServletWithMapping(String path, RESTServlet servlet);
	
	public void addCollectionWithMapping(String path, CollectionServlet coll, CollectionItemServlet item);
	
	public void start();
	
	public void stop();
	
	public void join();
  
}
