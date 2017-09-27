package de.thingweb.directory.resources;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;





import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.system.Txn;


import org.apache.jena.vocabulary.DC;

import de.thingweb.directory.ThingDirectory;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTResource;

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
