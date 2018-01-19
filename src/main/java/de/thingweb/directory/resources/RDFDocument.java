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
		
		RepositoryConnection conn = Connector.getRepositoryConnection();
		Resource res = ResourceFactory.createResource(uri);
		String ask = Queries.exists(res).serialize(Syntax.syntaxSPARQL_11);
		BooleanQuery q = conn.prepareBooleanQuery(ask);
		
		boolean exists = q.evaluate();
		if (!exists) {
			ThingDirectory.LOG.warn("Trying to create an empty, not persisted RDF document resource");
		}
	}
	
	public RDFDocument(String path, InputStream in) {
		this(path, new HashMap<>(), in);
	}
	
	public RDFDocument(String path, Map<String, String> parameters, InputStream in) {
		super(path, parameters);

		try {
			Model td = read(parameters, in);
			
			addDocument(uri, td);

			ThingDirectory.LOG.info(String.format("Added RDF document: %s (%d triples)", path, td.size()));
		} catch (RDFParseException | UnsupportedRDFormatException | IOException e) {
			ThingDirectory.LOG.error("Cannot parse RDF document", e);
		}
	}
	
	@Override
	public void get(Map<String, String> parameters, OutputStream out) throws RESTException {
		OutputStream sink = new ByteArrayOutputStream();
		super.get(parameters, sink); // in case an exception is thrown
		
		RepositoryConnection c = Connector.getRepositoryConnection();
		// TODO shorter call?
		String construct = String.format("CONSTRUCT { ?s ?p ?o } WHERE { GRAPH <%s> { ?s ?p ?o } }", uri);
		GraphQuery q = c.prepareGraphQuery(construct);
		
		Model m = QueryResults.asModel(q.evaluate());
		
		if (!m.isEmpty()) {			
			RDFFormat format = DEFAULT_RDF_FORMAT;
			if (parameters.containsKey(RESTResource.PARAMETER_ACCEPT)) {
				String mediaType = parameters.get(RESTResource.PARAMETER_ACCEPT);
				format = Rio.getParserFormatForMIMEType(mediaType).orElse(format);
			}
			// FIXME not thread-safe
			contentType = format.getDefaultMIMEType();
			
			Rio.write(m, out, format);
		} else {
			throw new NotFoundException();
		}
	}
	
	@Override
	public void put(Map<String, String> parameters, InputStream payload) throws RESTException {
		try {
			Model m = read(parameters, payload);
			
			// TODO single transaction
			removeDocument(uri);
			addDocument(uri, m);
			
			super.put(parameters, payload);
		} catch (RDFParseException | UnsupportedRDFormatException | IOException e) {
			throw new BadRequestException(e);
		}
	}
	
	@Override
	public void delete(Map<String, String> parameters) throws RESTException {
		super.delete(parameters);
		
		removeDocument(uri);
		
		ThingDirectory.LOG.info("Deleted RDF document: " + path);
	}
	
	private void addDocument(String uri, Model m) {
		// TODO remove method		
		RepositoryConnection conn = Connector.getRepositoryConnection();
		IRI res = conn.getValueFactory().createIRI(uri);
		conn.add(m, res);
	}

	private void removeDocument(String uri) {
		// TODO remove method
		RepositoryConnection conn = Connector.getRepositoryConnection();
		IRI res = conn.getValueFactory().createIRI(uri);
		conn.clear(res);
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
	
	protected static Model read(Map<String, String> parameters, InputStream payload) throws RDFParseException, UnsupportedRDFormatException, IOException {
		RDFFormat format = DEFAULT_RDF_FORMAT;
		if (parameters.containsKey(RESTResource.PARAMETER_CONTENT_TYPE)) {
			String mediaType = parameters.get(RESTResource.PARAMETER_CONTENT_TYPE);
			// TODO guess RDF specific type from generic media type (CoAP)
			format = Rio.getParserFormatForMIMEType(mediaType).orElse(format);
		}
		
		Model m = Rio.parse(payload, ThingDirectory.getBaseURI() + "/", format); // TODO take ep into account
		
		return m;
	}

}
