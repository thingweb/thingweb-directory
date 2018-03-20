package de.thingweb.directory.sparql.client;

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

import com.ontotext.graphdb.example.util.EmbeddedGraphDB;

import de.thingweb.directory.ThingDirectory;

public class Connector {
	
	private static RepositoryConnection connection;
	
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
		} catch (RepositoryConfigException | RepositoryException
				| RDFParseException | RDFHandlerException | IOException e) {
			// TODO throw instead?
			e.printStackTrace();
		}
	}
	
	public static RepositoryConnection getRepositoryConnection() {
		return connection;
	}

}
