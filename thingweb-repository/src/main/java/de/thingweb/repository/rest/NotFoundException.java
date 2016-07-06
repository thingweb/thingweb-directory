package de.thingweb.repository.rest;

import java.io.IOException;

public class NotFoundException extends RESTException {
	
	@Override
	public int getStatus() {
		return 404;
	}
	
}
