package de.thingweb.directory.rest;

public interface CollectionFilter {
	
	public boolean keep(RESTResource child);
	
}
