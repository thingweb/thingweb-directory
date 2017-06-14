package de.thingweb.repository.handlers;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

import de.thingweb.repository.Repository;
import de.thingweb.repository.rest.NotFoundException;
import de.thingweb.repository.rest.RESTException;
import de.thingweb.repository.rest.RESTHandler;
import de.thingweb.repository.rest.RESTResource;
import de.thingweb.repository.rest.RESTServerInstance;

public class OpenAPISpecHandler extends RESTHandler {
	
	public static final String FILENAME = "api.json";
	
	private static String sSpec;

	public OpenAPISpecHandler(List<RESTServerInstance> instances) {
		super(FILENAME, instances);
		
		if (sSpec == null) {
			// load at first instantiation
			sSpec = loadFile(FILENAME);
		}
	}

	@Override
	public RESTResource get(URI uri, Map<String, String> parameters) throws RESTException {
		if (sSpec != null) {
		    RESTResource spec = new RESTResource(FILENAME, this);
		    spec.content = sSpec;
		    spec.contentType = "application/json";
		    return spec;
		} else {
			throw new NotFoundException();
		}
	}
	
	private String loadFile(String filename) {
        InputStream in = getClass().getClassLoader().getResourceAsStream(filename);
        byte[] buf = new byte[8192]; // FIXME arbitrary length

        try {
			int length = in.read(buf);
	        return new String(buf, 0, length);
		} catch (IOException e) {
			e.printStackTrace();
		}
        
		return null;
	}
	
}
