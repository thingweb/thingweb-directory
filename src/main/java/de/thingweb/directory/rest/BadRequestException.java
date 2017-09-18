package de.thingweb.directory.rest;

import java.io.IOException;

public class BadRequestException extends RESTException {
	
	@Override
	public int getStatus() {
		return 400;
	}
	
}
