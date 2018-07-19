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

import java.io.IOException;

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.thingweb.directory.ThingDirectory;
import org.eclipse.thingweb.directory.graphdb.EmbeddedGraphDB;

public class Connector {
	
	private static RepositoryConnection connection;
	
	private Connector() {
		// never used
	}
	
	public static void init(String queryEndpoint, String updateEndpoint, String username, String password) {
		SPARQLRepository repo = new SPARQLRepository(queryEndpoint, updateEndpoint);
		repo.initialize();
		repo.setUsernameAndPassword(username, password);
		
		connection = repo.getConnection();
		
		try {
			// probe to test SPARQL endpoint availability
			connection.isEmpty();
			// TODO request SPARQL service description and check for sd:UnionDefaultGraph
		} catch (RepositoryException e) {
			ThingDirectory.LOG.warn("SPARQL endpoint cannot be reached. Switching to main memory RDF store...");
			init();
		}
		
		// TODO close connection
	}
	
	public static void init(String queryEndpoint, String updateEndpoint) {
		init(queryEndpoint, updateEndpoint, null, null);
	}
	
	public static void init() {
		try {
			connection = EmbeddedGraphDB.openConnectionToTemporaryRepository("owl2-rl-optimized");
		} catch (IOException e) {
			ThingDirectory.LOG.error("Could not initialize embedded GraphDB", e);
		}
	}
	
	public static RepositoryConnection getRepositoryConnection() {
		if (connection == null) {
			// try again in case IO error was temporary
			// TODO what about remote endpoint init?
			init();
		}
		
		return connection;
	}

}
