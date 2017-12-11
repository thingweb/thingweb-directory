package de.thingweb.directory.rest;


public class BadRequestException extends RESTException {
	
	@Override
	public int getStatus() {
		return 400;
	}
	
}
