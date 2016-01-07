package de.thingweb.repository.rest;

public class MethodNotAllowedException extends RESTException {

	@Override
	public int getStatus() {
		return 405;
	}
	
}
