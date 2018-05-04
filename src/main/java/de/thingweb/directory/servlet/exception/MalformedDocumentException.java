package de.thingweb.directory.servlet.exception;

import java.io.IOException;

public class MalformedDocumentException extends IOException {

	public MalformedDocumentException() {
		super();
	}

	public MalformedDocumentException(Throwable cause) {
		super(cause);
	}

	public MalformedDocumentException(String message) {
		super(message);
	}

	public MalformedDocumentException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
