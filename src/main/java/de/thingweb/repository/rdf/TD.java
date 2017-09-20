package de.thingweb.repository.rdf;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public class TD {
	
	public static final Resource Thing = ResourceFactory.createResource(getURI() + "Thing");

	public static String getURI() {
		return "http://iot.linkeddata.es/def/wot#"; // TODO to update
	}
	
}
