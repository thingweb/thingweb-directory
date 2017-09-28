package de.thingweb.directory.resources;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import de.thingweb.directory.rest.IndexResource;
import de.thingweb.directory.rest.MethodNotAllowedException;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTResource;

public class WelcomePageResource extends IndexResource {
	
	public WelcomePageResource() {
		super(null);
		
		children.add(new TDCollectionResource("/td"));
		children.add(new VocabularyCollectionResource("/vocab"));
		// TODO ns-lookup
		// TODO Open API Spec.
	}
	
	@Override
	public void get(Map<String, String> parameters, OutputStream out) throws RESTException {
		// TODO return HTML page
		super.get(parameters, out);
	}
	
	@Override
	public RESTResource post(Map<String, String> parameters, InputStream payload) throws RESTException {
		throw new MethodNotAllowedException();
	}

}
