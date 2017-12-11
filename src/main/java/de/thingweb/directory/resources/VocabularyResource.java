package de.thingweb.directory.resources;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class VocabularyResource extends RDFDocument {
	
	public VocabularyResource(String path, InputStream in) {
		this(path, new HashMap<>(), in);
	}
	
	public VocabularyResource(String path, Map<String, String> parameters, InputStream in) {
		super(path, parameters, in);
	}

}
