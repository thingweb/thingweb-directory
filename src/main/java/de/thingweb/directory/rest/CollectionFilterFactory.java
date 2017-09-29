package de.thingweb.directory.rest;

import java.util.Map;

public interface CollectionFilterFactory {
	
	public CollectionFilter create(Map<String, String> parameters);

}
