package de.thingweb.directory.resources;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import de.thingweb.directory.rest.CollectionResource;
import de.thingweb.directory.rest.IndexResource;
import de.thingweb.directory.rest.MethodNotAllowedException;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTResource;

public class TDLookUpCollectionResource extends CollectionResource {
	
	public TDLookUpCollectionResource() {
		super("/td-lookup", null);
		
		children.add(new TDLookUpResResource());
		children.add(new TDLookUpEpResource());
		children.add(new TDLookUpSemResource());
	}
	
	@Override
	public RESTResource post(Map<String, String> parameters, InputStream payload) throws RESTException {
		throw new MethodNotAllowedException();
	}

}
