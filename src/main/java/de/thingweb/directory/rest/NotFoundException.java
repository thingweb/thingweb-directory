package de.thingweb.directory.rest;


public class NotFoundException extends RESTException {
	
	@Override
	public int getStatus() {
		return 404;
	}
	
}
