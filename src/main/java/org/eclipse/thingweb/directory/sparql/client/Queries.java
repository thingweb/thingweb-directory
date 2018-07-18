/********************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
 * 
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the W3C Software Notice and
 * Document License (2015-05-13) which is available at
 * https://www.w3.org/Consortium/Legal/2015/copyright-software-and-document.
 * 
 * SPDX-License-Identifier: EPL-2.0 OR W3C-20150513
 ********************************************************************************/
package org.eclipse.thingweb.directory.sparql.client;

import java.time.LocalDateTime;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;

public class Queries {
	
	// TODO replace second parameter by 'now()'?
	private static final String HAS_EXPIRED_TPL = "ASK WHERE { "
			+ "<%s> <http://purl.org/dc/terms/valid> ?date "
			+ "FILTER (%s > ?date) "
			+ "}";
	
	private static final String CREATE_TIMEOUT_TPL = "INSERT DATA { "
			+ "<%s> <http://purl.org/dc/terms/created> %s ; "
			+ "<http://purl.org/dc/terms/modified> %s ; "
			+ "<http://purl.org/dc/terms/valid> %s "
			+ "}";
	
	private static final String UPDATE_TIMEOUT_TPL = "DELETE { "
			+ "<%s> <http://purl.org/dc/terms/modified> ?modified ; "
			+ "<http://purl.org/dc/terms/valid> ?valid "
			+ "} INSERT { "
			+ "<%s> <http://purl.org/dc/terms/modified> %s ; "
			+ "<http://purl.org/dc/terms/valid> %s "
			+ "} WHERE { "
			+ "<%s> <http://purl.org/dc/terms/modified> ?modified ; "
			+ "<http://purl.org/dc/terms/valid> ?valid "
			+ "}";
	
	private static final String LOAD_RESOURCE_TPL = "";
	
	private static final String UPDATE_RESOURCE_TPL = "";
	
	private static final String DELETE_RESOURCE_TPL = "DROP <%s>";
	
	private static final String EXISTS_TPL = "ASK WHERE { "
			+ "GRAPH <%s> { "
			+ "?s ?p ?o "
			+ "} "
			+ "}";
	
	private static final String GET_RESOURCE_TPL = "CONSTRUCT { "
			+ "?s ?p ?o "
			+ "} WHERE { "
			+ "GRAPH <%s> { "
			+ "?s ?p ?o "
			+ "}"
			+ "}";
	
	private static final String LIST_RESOURCES_TPL = "SELECT DISTINCT ?res WHERE { "
			+ "GRAPH ?res { "
			+ "%s " // arbitrary graph pattern
			+ "} "
			+ "}";
	
	private Queries() {
		// never used
	}
	
	/**
	 * ASK WHERE {
	 *   ?res dct:valid ?date
	 *   FILTER (now() > ?date)
	 * }
	 * 
	 * @param res
	 * @return
	 */
	public static boolean hasExpired(String res) {
		RepositoryConnection conn = Connector.getRepositoryConnection();
		
		Literal now = getDateTime(0, conn.getValueFactory());
		
		String q = String.format(HAS_EXPIRED_TPL, res, now);
		
		return conn.prepareBooleanQuery(q).evaluate();
	}
	
	/**
	 * INSERT DATA {
	 *   ?res dct:created now() .
	 *   ?res dct:modified now() .
	 *   ?res dct:valid now() + ?lifetime .
	 * }
	 * 
	 * @param res
	 * @param lifetime
	 * @return
	 */
	public static void createTimeout(String res, int lifetime) {
		RepositoryConnection conn = Connector.getRepositoryConnection();
		
		Literal now = getDateTime(0, conn.getValueFactory());
		Literal timeout = getDateTime(lifetime, conn.getValueFactory());

		String u = String.format(CREATE_TIMEOUT_TPL, res, now, now, timeout);
		
		conn.prepareUpdate(u).execute();
	}
	
	/**
	 * DELETE {
	 *   ?res dct:modified ?modified ;
	 *        dct:valid ?valid .
	 * } INSERT {
	 *   ?res dct:modified now() ;
	 *        dct:valid now() + ?lifetime .
	 * } WHERE {
	 *   ?res dct:modified ?modified ;
	 *        dct:valid ?valid .
	 * }
	 * 
	 * @param res
	 * @param lifetime
	 * @return
	 */
	public static void updateTimeout(String res, int lifetime) {
		RepositoryConnection conn = Connector.getRepositoryConnection();
		
		Literal now = getDateTime(0, conn.getValueFactory());
		Literal timeout = getDateTime(lifetime, conn.getValueFactory());

		String u = String.format(UPDATE_TIMEOUT_TPL, res, res, now, timeout, res);
		
		conn.prepareUpdate(u).execute();
	}
	
	/**
	 * INSERT DATA {
	 *   GRAPH ?res {
	 *     ...
	 *   }
	 * }
	 * 
	 * @param id
	 * @param m
	 * @return
	 */
	public static void loadResource(String res, Model m) {
		// TODO add rdfs:isDefinedBy statement when loading?
		RepositoryConnection conn = Connector.getRepositoryConnection();
		IRI iri = conn.getValueFactory().createIRI(res);
		conn.add(m, iri);
	}
	
	/**
	 * DROP ?id
	 * 
	 * @param g
	 * @return
	 */
	public static void deleteResource(String res) {		
		RepositoryConnection conn = Connector.getRepositoryConnection();
		IRI iri = conn.getValueFactory().createIRI(res);
		conn.clear(iri);
	}
	
	/**
	 * DROP ?res,
	 * INSERT DATA {
	 *   GRAPH ?res {
	 *     ...
	 *   }
	 * }
	 * 
	 * @param id
	 * @param m
	 * @return
	 */
	public static void replaceResource(String res, Model m) {
		// TODO put in single transaction
		deleteResource(res);
		loadResource(res, m);
	}
	
	/**
	 * ASK WHERE {
	 *   GRAPH ?res {
	 *     ?s ?p ?o
	 *   }
	 * }
	 * 
	 * @param id
	 * @return
	 */
	public static boolean exists(String res) {
		String q = String.format(EXISTS_TPL, res);
		
		RepositoryConnection conn = Connector.getRepositoryConnection();
		return conn.prepareBooleanQuery(q).evaluate();
	}
	
	/**
	 * CONSTRUCT {
	 *   ?s ?p ?o
	 * } WHERE {
	 *   GRAPH ?res {
	 *     ?s ?p ?o .
	 *   }
	 * }
	 * 
	 * @param id
	 * @return
	 */
	public static Model getResource(String res) {
		// TODO filter out rdfs:isDefinedBy?
		String q = String.format(GET_RESOURCE_TPL, res);
		
		RepositoryConnection conn = Connector.getRepositoryConnection();
		GraphQuery gq = conn.prepareGraphQuery(q);
		
		return QueryResults.asModel(gq.evaluate());
	}

	/**
	 * SELECT ?res WHERE {
	 *   GRAPH ?res {
	 *     ...
	 *   }
	 * }
	 * 
	 * @param SPARQL graph pattern
	 * @return
	 */
	public static TupleQueryResult listResources(String pattern) {
		// TODO query against union graph instead?
		String q = String.format(LIST_RESOURCES_TPL, pattern);
		
		RepositoryConnection conn = Connector.getRepositoryConnection();
		return conn.prepareTupleQuery(q).evaluate();
	}

	private static Literal getDateTime(int lifetime, ValueFactory factory) {
		LocalDateTime d = LocalDateTime.now();
		d = d.plusSeconds(lifetime);
		
		// ISO 8601 string format
		return factory.createLiteral(d.toString(), XMLSchema.DATETIME);
	}
	
}
