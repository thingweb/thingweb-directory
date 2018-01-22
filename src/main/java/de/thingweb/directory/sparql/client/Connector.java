package de.thingweb.directory.sparql.client;

import java.io.IOException;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.system.Txn;
import org.apache.jena.tdb.TDBFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;

import com.ontotext.graphdb.example.util.EmbeddedGraphDB;

public class Connector {
	
	private static Dataset dataset = null;
	
	private static String endpoint = null;
	
	private static RepositoryConnection connection;
	
	public static void init(String queryEndpoint, String updateEndpoint) {
		Repository repo = new SPARQLRepository(queryEndpoint, updateEndpoint);
		repo.initialize();
		connection = repo.getConnection();
		// TODO close connection
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
