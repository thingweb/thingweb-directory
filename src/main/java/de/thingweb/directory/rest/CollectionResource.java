package de.thingweb.directory.rest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import de.thingweb.directory.ThingDirectory;

public class CollectionResource extends RESTResource {

	protected final RESTResourceFactory factory;
	protected Set<RESTResource> children = new HashSet<>();

	public CollectionResource(String path, RESTResourceFactory f) {
		super(path);
		factory = f;
		contentType = "application/json";
	}

	@Override
	public void get(Map<String, String> parameters, OutputStream out) throws RESTException {
		try {
			out.write('[');
			
			Iterator<RESTResource> it = children.iterator();
			while (it.hasNext()) {
				RESTResource res = it.next();
				out.write('"');
				out.write(res.getName().getBytes());
				out.write('"');
				if (it.hasNext()) {
					out.write(',');
				}
			}
			
			out.write(']');
		} catch (IOException e) {
			ThingDirectory.LOG.error("Cannot write byte array", e);
			throw new RESTException();
		}
	}

	@Override
	public RESTResource post(Map<String, String> parameters, InputStream payload) throws RESTException {
		RESTResource child = factory.create(path + "/" + generateChildID(), parameters, payload);
		children.add(child);
		return child;
	}
	
	protected String generateChildID() {
		String id = UUID.randomUUID().toString();
		return id.substring(0, id.indexOf('-'));
	}

}
