package de.thingweb.directory.resources;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;


import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFErrorHandler;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.system.Txn;
import org.apache.jena.update.Update;
import org.apache.jena.update.UpdateRequest;


import de.thingweb.directory.ThingDirectory;
import de.thingweb.directory.rest.NotFoundException;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTResource;
import de.thingweb.directory.rest.RESTResourceFactory;
import de.thingweb.directory.sparql.client.Queries;

public class RDFDocument extends DirectoryResource {
	
	public final static String DEFAULT_FORMAT = "JSON-LD";
	
	public RDFDocument(String path, InputStream in) {
		this(path, new HashMap<>(), in);
	}
	
	public RDFDocument(String path, Map<String, String> parameters, InputStream in) {
		super(path, parameters);

		Model td = read(parameters, in);
		try (RDFConnection conn = ThingDirectory.get().getStoreConnection()) {
			Txn.executeWrite(conn, () -> {
				addDocument(uri, td, conn);
			});
		}

		ThingDirectory.LOG.info(String.format("Added RDF document: %s (%d triples)", path, td.size()));
	}
	
	@Override
	public void get(Map<String, String> parameters, OutputStream out) throws RESTException {
		OutputStream sink = new ByteArrayOutputStream();
		super.get(parameters, sink); // in case an exception is thrown
		
		try (RDFConnection conn = ThingDirectory.get().getStoreConnection()) {
			boolean found = Txn.calculateRead(conn, () -> {
				Model m = conn.fetch(uri);
				
				if (m.isEmpty()) {
					return false; // resource not found
				}

				String format = DEFAULT_FORMAT;
				if (parameters.containsKey(RESTResource.PARAMETER_ACCEPT)) {
					String mediaType = parameters.get(RESTResource.PARAMETER_ACCEPT);
					format = getFormat(mediaType);
				}
				
				m.write(out, format);
				
				return true;
			});
			
			if (!found) {
				throw new NotFoundException();
			}
		}
	}
	
	@Override
	public void put(Map<String, String> parameters, InputStream payload) throws RESTException {
		String format = DEFAULT_FORMAT;
		if (parameters.containsKey(RESTResource.PARAMETER_CONTENT_TYPE)) {
			String mediaType = parameters.get(RESTResource.PARAMETER_CONTENT_TYPE);
			format = getFormat(mediaType);
		}

		Model m = ModelFactory.createDefaultModel();
		m.read(payload, "", format);
		
		try (RDFConnection conn = ThingDirectory.get().getStoreConnection()) {
			Txn.executeWrite(conn, () -> {
				removeDocument(uri, conn);
				addDocument(uri, m, conn);
			});
		}
		
		super.put(parameters, payload);
	}
	
	@Override
	public void delete(Map<String, String> parameters) throws RESTException {
		super.delete(parameters);
		
		try (RDFConnection conn = ThingDirectory.get().getStoreConnection()) {
			Txn.executeWrite(conn, () -> {
				removeDocument(uri, conn);
			});
		}
	}
	
	private void addDocument(String uri, Model m, RDFConnection conn) {
		Resource res = ResourceFactory.createResource(uri);
		
		Update up = Queries.loadGraph(res, m);
		conn.update(up);
	}

	private void removeDocument(String uri, RDFConnection conn) {
		Resource res = ResourceFactory.createResource(uri);
		
		UpdateRequest up = Queries.deleteGraph(res);
		conn.update(up);
	}
	
	public static RESTResourceFactory factory() {
		return new RESTResourceFactory() {
			
			@Override
			public RESTResource create(String path) {
				return null; // TODO throw exception?
			}
			
			@Override
			public RESTResource create(String path, Map<String, String> parameters) {
				return null; // TODO throw exception?
			}
			
			@Override
			public RESTResource create(String path, InputStream payload) {
				return new RDFDocument(path, payload);
			}
			
			@Override
			public RESTResource create(String path, Map<String, String> parameters, InputStream payload) {
				return new RDFDocument(path, parameters, payload);
			}
			
		};
	}
	
	protected static Model read(Map<String, String> parameters, InputStream payload) {
		String format = DEFAULT_FORMAT;
		if (parameters.containsKey(RESTResource.PARAMETER_CONTENT_TYPE)) {
			String mediaType = parameters.get(RESTResource.PARAMETER_CONTENT_TYPE);
			format = getFormat(mediaType);
		}
		
		Model m = ModelFactory.createDefaultModel();
		m.read(payload, ThingDirectory.get().getBaseURI(), format);
		
		return m;
	}
	
	private static String getFormat(String mediaType) {
		Lang l = RDFLanguages.contentTypeToLang(mediaType);
		if (l != null) {
			return l.getName();
		} else {
			// TODO guess RDF specific type from generic media type (CoAP)
			ThingDirectory.LOG.debug("No RDF format for media type: " + mediaType + ". Assuming JSON-LD.");
			return DEFAULT_FORMAT;
		}
	}

}
