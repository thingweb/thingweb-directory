package de.thingweb.directory.rest;

import java.io.InputStream;
import java.util.Map;

public interface RESTResourceFactory {

	public RESTResource create(String path);
	public RESTResource create(String path, InputStream payload);
	public RESTResource create(String path, Map<String, String> parameters);
	public RESTResource create(String path, Map<String, String> parameters, InputStream payload);
	
}
