package de.thingweb.directory.rest;

public class RESTResource {
	
	public String name;
	public String path;
	public String contentType;
	public String content;
	
	public RESTHandler handler;
	
	public RESTResource(String path, RESTHandler handler) {
		this.path = path;
		if (path.contains("/")) {
		  this.name = path.substring(path.lastIndexOf('/') + 1);
		} else {
		  this.name = path;
		}
		this.handler =  handler;
	}
	
}
