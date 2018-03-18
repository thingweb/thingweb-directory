package de.thingweb.directory.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;

import com.github.jsonldjava.utils.JsonUtils;

import de.thingweb.directory.ThingDirectory;
import de.thingweb.directory.rest.RESTServlet;
import de.thingweb.directory.sparql.client.Queries;

public class RDFDocumentServlet extends RegistrationResourceServlet {
	
	public final static String DEFAULT_FORMAT = "JSON-LD";

	public final static RDFFormat DEFAULT_RDF_FORMAT = RDFFormat.JSONLD;
	
	public final static String DEFAULT_MEDIA_TYPE = "application/ld+json";
	
	private final static String[] ACCEPTED = {
		"application/n-triples",
		"text/turtle",
		"application/rdf+xml",
		"application/ld+json"
	};

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		super.doGet(req, resp);

		String uri = getRDFDocumentURI(getItemID(req));
		Model m = Queries.getResource(uri);
		
		if (!m.isEmpty()) {
			writeContent(m, req, resp);
		} else {
			resp.sendError(404); // Not Found
		}
	}
	
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		super.doPut(req, resp);
		
		try {
			Model m = Rio.parse(req.getInputStream(), getBaseURI(req), getContentFormat(req));

			String uri = getRDFDocumentURI(getItemID(req));
			Queries.replaceResource(uri, m);
		} catch (RDFParseException | UnsupportedRDFormatException | IOException e) {
			throw new ServletException(e);
		}
	}
	
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		super.doDelete(req, resp);

		String uri = getRDFDocumentURI(getItemID(req));
		Queries.deleteResource(uri);
		
		ThingDirectory.LOG.info("Deleted RDF document: " + uri);
	}
	
	@Override
	protected String doAdd(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			Model m = Rio.parse(req.getInputStream(), getBaseURI(req), getContentFormat(req));
			
			String id = generateItemID(m);
			
			String uri = getRDFDocumentURI(id);
			Queries.loadResource(uri, m);
			
			// TODO normalize RDF graph (by giving URIs to blank nodes)

			ThingDirectory.LOG.info(String.format("Added RDF document: %s (%d triples)", uri, m.size()));
			
			return id;
		} catch (RDFParseException | UnsupportedRDFormatException | IOException e) {
			ThingDirectory.LOG.error("Cannot parse RDF document", e);
			throw new ServletException(e);
		}
	}
	
	/**
	 * Alternative to generateItemID()
	 * 
	 * @param m
	 * @return
	 */
	protected String generateItemID(Model m) throws ServletException {
		return super.generateItemID();
	}
	
	@Override
	protected boolean hasExpired(String id) {
		return Queries.hasExpired(getRDFDocumentURI(id));
	}

	@Override
	protected void updateTimeout(String id, int lifetime) {
		Queries.updateTimeout(getRDFDocumentURI(id), lifetime);
	}
	
	protected String getRDFDocumentURI(String id) {
		// TODO better scheme?
		return "urn:" + id;
	}
	
	protected String getItemId(String documentURI) {
		return documentURI.substring(4); // remove 'urn:'
	}
	
	protected RDFFormat getContentFormat(HttpServletRequest req) {
		RDFFormat format = DEFAULT_RDF_FORMAT;
		
		if (req.getContentType() != null) {
			String mediaType = req.getContentType();
			// TODO guess RDF specific type from generic media type (CoAP)
			format = Rio.getParserFormatForMIMEType(mediaType).orElse(format);
		}
		
		return format;
	}
	
	protected RDFFormat getAcceptedFormat(HttpServletRequest req) {
		RDFFormat format = DEFAULT_RDF_FORMAT;
		
		if (req.getHeader(RESTServlet.ACCEPT_HEADER) != null) {
			String mediaType = req.getHeader(RESTServlet.ACCEPT_HEADER);
			format = Rio.getParserFormatForMIMEType(mediaType).orElse(format);
		}
		
		return format;
	}
	
	/**
	 * To be overridden by subclasses if post-processing is required (e.g. JSON-LD framing).
	 * 
	 * @param m RDF document as an RDF4J model
	 * @param req servlet request
	 * @param resp servlet response
	 * @throws RDFHandlerException
	 * @throws IOException
	 */
	protected void writeContent(Model m, HttpServletRequest req, HttpServletResponse resp) throws RDFHandlerException, IOException {
		RDFFormat format = getAcceptedFormat(req);
		resp.setContentType(format.getDefaultMIMEType());
		Rio.write(m, resp.getOutputStream(), format);
	}
	
	@Override
	protected String[] getAcceptedContentTypes() {
		// TODO get all media types from Rio?
		return ACCEPTED;
	}

}
