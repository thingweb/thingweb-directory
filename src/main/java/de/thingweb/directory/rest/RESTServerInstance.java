package de.thingweb.directory.rest;

/**
 * 
 * A server instance should:
 *  - forward requests to and forward responses from resources in the resource tree
 *  - listen to changes in the resource tree (creation/deletion)
 *
 * @author Victor Charpenay
 * @creation 28.09.2017
 *
 */
public interface RESTServerInstance extends RESTResourceListener {

	public void setIndex(IndexResource index);
	
	public void start();
	
	public void stop();
	
	public void join();
  
}
