package de.thingweb.directory.rest;

public class RESTException extends Exception {
	
	public RESTException() {
		super();
	}
	
	public RESTException(String message) {
		super(message);
	}
	
	public RESTException(Throwable throwable) {
		super(throwable);
	}
	
	public RESTException(String message, Throwable throwable) {
		super(message, throwable);
	}
	
	public int getStatus() {
		return 500;
	}
	
}
