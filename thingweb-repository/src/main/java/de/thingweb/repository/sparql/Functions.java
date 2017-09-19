package de.thingweb.repository.sparql;

import org.apache.jena.sparql.function.FunctionRegistry;

/**
 * 
 * Implements WoT API functions via SPARQL extension functions. See:
 * https://www.w3.org/TR/sparql11-query/#extensionFunctions
 *
 * @author Victor Charpenay
 * @creation 25.08.2017
 *
 */
public class Functions {
	
	public static final String NS = "http://iot.linkeddata.es/def/wot#";
	
	public static void registerAll() {
		FunctionRegistry.get().put(NS + "getProperty", GetProperty.class);
	}

}
