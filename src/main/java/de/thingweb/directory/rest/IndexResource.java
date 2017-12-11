package de.thingweb.directory.rest;

import java.util.Map;

/**
 * 
 * API Entry point.
 *
 * @author Victor Charpenay
 * @creation 28.09.2017
 *
 */
public class IndexResource extends CollectionResource implements RESTResourceListener {

	Map<String, RESTResource> index;
	
	public IndexResource(RESTResourceFactory factory) {
		super("/", factory);
	}
	
	@Override
	public void onCreate(RESTResource resource) {
		index.put(resource.getPath(), resource);
	}
	
	@Override
	public void onDelete(RESTResource resource) {
		index.remove(resource.getPath());
	}
	
	public RESTResource find(String path) {
		return index.get(path);
	}
	
}
