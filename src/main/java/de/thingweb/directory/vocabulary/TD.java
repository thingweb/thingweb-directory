package de.thingweb.directory.vocabulary;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class TD {

	public static final Resource Thing = SimpleValueFactory.getInstance().createIRI(getURI() + "Thing");

	public static String getURI() {
		return "http://www.w3.org/ns/td#";
	}
	
}
