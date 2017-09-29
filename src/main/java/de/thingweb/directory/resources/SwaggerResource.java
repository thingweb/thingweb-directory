package de.thingweb.directory.resources;

import de.thingweb.directory.rest.RESTResource;

public class SwaggerResource extends RESTResource {
	
	public static final String FILENAME = "api.json";

	public SwaggerResource() {
		super("/api", SwaggerResource.class.getClassLoader().getResourceAsStream(FILENAME));
		contentType = "application/json"; // FIXME not sent in the response?

		// TODO use Java annotations instead
	}
	
}
