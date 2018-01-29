package de.thingweb.directory.resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import de.thingweb.directory.ThingDirectory;
import de.thingweb.directory.rest.IndexResource;
import de.thingweb.directory.rest.MethodNotAllowedException;
import de.thingweb.directory.rest.NotFoundException;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTResource;

public class WelcomePageResource extends IndexResource {

	private static final String CONTENT_TYPE = "text/html";
	
	// TODO static?
	private String buffer;
	
	public WelcomePageResource() {
		super(null);
		
		ClassLoader cl = getClass().getClassLoader();
		try {
			buffer = readAll(cl.getResourceAsStream("index.html"));
		} catch (IOException e) {
			buffer = null;
			ThingDirectory.LOG.error("Cannot fetch HTML welcome page", e);
		}
		
		TDCollectionResource tds = new TDCollectionResource();
		
		children.add(tds);
		children.add(new TDLookUpCollectionResource(tds));
		children.add(new VocabularyCollectionResource());
		children.add(new SwaggerResource());
	}
	
	@Override
	public void get(Map<String, String> parameters, OutputStream out) throws RESTException {
		try {
			contentType = CONTENT_TYPE;
			out.write(buffer.getBytes());
		} catch (IOException e) {
			throw new RESTException(e);
		} catch (NullPointerException e) {
			throw new NotFoundException();
		}
	}
	
	@Override
	public RESTResource post(Map<String, String> parameters, InputStream payload) throws RESTException {
		throw new MethodNotAllowedException();
	}
	
	private String readAll(InputStream in) throws IOException {		
		byte[] b = new byte[16384]; // greater than size of index.html
		int len = in.read(b);
		
		return new String(b, 0, len);
	}

}
