package de.thingweb.directory.rest;


public class BadRequestException extends RESTException {
	
	public BadRequestException() {
		super();
	}
	
	public BadRequestException(String message) {
		super(message);
	}
	
	public BadRequestException(Throwable throwable) {
		super(throwable);
	}
	
	public BadRequestException(String message, Throwable throwable) {
		super(message, throwable);
	}
	
	@Override
	public int getStatus() {
		return 400;
	}
	
}
