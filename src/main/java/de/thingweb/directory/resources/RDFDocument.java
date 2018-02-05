package de.thingweb.directory.resources;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;

import de.thingweb.directory.ThingDirectory;
import de.thingweb.directory.rest.BadRequestException;
import de.thingweb.directory.rest.NotFoundException;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTResource;
import de.thingweb.directory.rest.RESTResourceFactory;
import de.thingweb.directory.sparql.client.Connector;
import de.thingweb.directory.sparql.client.Queries;

public class RDFDocument extends DirectoryResource {
	
	public final static String DEFAULT_FORMAT = "JSON-LD";

	public final static RDFFormat DEFAULT_RDF_FORMAT = RDFFormat.JSONLD;
	
	public final static String DEFAULT_MEDIA_TYPE = "application/ld+json";
	
	public RDFDocument(String path) {
		this(path, new HashMap<>());
	}
	
	public RDFDocument(String path, Map<String, String> parameters) {
		super(path, parameters);
		
		if (!Queries.exists(uri)) {
			ThingDirectory.LOG.warn("Trying to create an empty, not persisted RDF document resource");
		}
	}
	
	public RDFDocument(String path, InputStream in) {
		this(path, new HashMap<>(), in);
	}
	
	public RDFDocument(String path, Map<String, String> parameters, InputStream in) {
		super(path, parameters);

		try {
			Model m = Rio.parse(in, getInputBaseURI(parameters), getInputContentType(parameters));
			
			Queries.loadResource(uri, m);

			ThingDirectory.LOG.info(String.format("Added RDF document: %s (%d triples)", path, m.size()));
		} catch (RDFParseException | UnsupportedRDFormatException | IOException e) {
			ThingDirectory.LOG.error("Cannot parse RDF document", e);
		}
	}
	
	@Override
	public void get(Map<String, String> parameters, OutputStream out) throws RESTException {
		OutputStream sink = new ByteArrayOutputStream();
		super.get(parameters, sink); // in case an exception is thrown
		
		Model m = Queries.getResource(uri);
		
		if (!m.isEmpty()) {
			RDFFormat format = getOutputAccept(parameters);
			Rio.write(m, out, format);
			// FIXME not thread-safe
			contentType = format.getDefaultMIMEType();
		} else {
			throw new NotFoundException();
		}
	}
	
	@Override
	public void put(Map<String, String> parameters, InputStream payload) throws RESTException {
		try {
			Model m = Rio.parse(payload, getInputBaseURI(parameters), getInputContentType(parameters));
			
			Queries.replaceResource(uri, m);
			
			super.put(parameters, payload);
		} catch (RDFParseException | UnsupportedRDFormatException | IOException e) {
			throw new BadRequestException(e);
		}
	}
	
	@Override
	public void delete(Map<String, String> parameters) throws RESTException {
		super.delete(parameters);
		
		Queries.deleteResource(uri);
		
		ThingDirectory.LOG.info("Deleted RDF document: " + path);
	}
	
	public static RESTResourceFactory factory() {
		return new RESTResourceFactory() {
			
			@Override
			public RESTResource create(String path) {
				return new RDFDocument(path);
			}
			
			@Override
			public RESTResource create(String path, Map<String, String> parameters) {
				return new RDFDocument(path, parameters);
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
	
	protected static String getInputBaseURI(Map<String, String> parameters) {
		// TODO take ep into account
		return ThingDirectory.getBaseURI() + "/";
	}
	
	protected static RDFFormat getInputContentType(Map<String, String> parameters) {
		RDFFormat format = DEFAULT_RDF_FORMAT;
		
		if (parameters.containsKey(RESTResource.PARAMETER_CONTENT_TYPE)) {
			String mediaType = parameters.get(RESTResource.PARAMETER_CONTENT_TYPE);
			// TODO guess RDF specific type from generic media type (CoAP)
			format = Rio.getParserFormatForMIMEType(mediaType).orElse(format);
		}
		
		return format;
	}
	
	protected static RDFFormat getOutputAccept(Map<String, String> parameters) {
		RDFFormat format = DEFAULT_RDF_FORMAT;
		
		if (parameters.containsKey(RESTResource.PARAMETER_ACCEPT)) {
			String mediaType = parameters.get(RESTResource.PARAMETER_ACCEPT);
			format = Rio.getParserFormatForMIMEType(mediaType).orElse(format);
		}
		
		return format;
	}

}
