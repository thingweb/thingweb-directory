package de.thingweb.directory.resources;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import de.thingweb.directory.rest.RESTException;

public class TDResource extends RDFDocument {
	
	public TDResource(String path, InputStream in) {
		this(path, new HashMap<>(), in);
	}
	
	public TDResource(String path, Map<String, String> parameters, InputStream in) {
		super(path, parameters, in);
		
		// TODO triple materialization?
	}
	
	@Override
	public void get(Map<String, String> parameters, OutputStream out) throws RESTException {
		super.get(parameters, out);
		
		// TODO JSON-LD framing
	}

}
