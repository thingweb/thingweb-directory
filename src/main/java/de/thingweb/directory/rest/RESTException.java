package de.thingweb.directory.rest;

import java.io.IOException;

public class RESTException extends IOException {
	
	public int getStatus() {
		return 500;
	}
	
}
