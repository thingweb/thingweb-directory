package de.thingweb.directory.resources;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import de.thingweb.directory.rest.MethodNotAllowedException;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTResource;

public class TDLookUpSemResource extends RESTResource {
	
	private final TDCollectionResource tds;

	public TDLookUpSemResource(TDCollectionResource tds) {
		super("/td-lookup/sem");
		
		this.tds = tds;
	}
	
	@Override
	public void get(Map<String, String> parameters, OutputStream out) throws RESTException {
		tds.get(parameters, out);
	}
	
	@Override
	public RESTResource post(Map<String, String> parameters, InputStream payload) throws RESTException {
		throw new MethodNotAllowedException();
	}
	
}
