package de.thingweb.directory.rest;

import java.io.IOException;

public class NotFoundException extends RESTException {
	
	@Override
	public int getStatus() {
		return 404;
	}
	
}
