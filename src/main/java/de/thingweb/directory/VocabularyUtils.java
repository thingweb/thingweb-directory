package de.thingweb.directory;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.system.Txn;

import de.thingweb.directory.sparql.client.Connector;

public class VocabularyUtils {

//	public static boolean containsVocabulary(String uri) {
//		Dataset dataset = ThingDirectory.get().dataset;
//		boolean isOpen = dataset.isInTransaction();
//		if (!isOpen) {
//			dataset.begin(ReadWrite.READ);
//		}
//
//		try {
//			String q = "ASK { GRAPH ?g { <%s> a <http://www.w3.org/2002/07/owl#Ontology> } }";
//			QueryExecution qexec = QueryExecutionFactory.create(String.format(q, uri), dataset);
//			return qexec.execAsk();
//		} catch (Exception e) {
//			throw e;
//		} finally {
//			if (!isOpen) {
//				dataset.end();
//			}
//		}
//	}
//
//	public static Set<String> listVocabularies() {
//		Set<String> tds = new HashSet<>();
//		Dataset dataset = ThingDirectory.get().dataset;
//		dataset.begin(ReadWrite.READ);
//
//		try {
//			String q = "SELECT DISTINCT ?g WHERE { GRAPH ?g { ?o a <http://www.w3.org/2002/07/owl#Ontology> } }";
//			QueryExecution qexec = QueryExecutionFactory.create(q, dataset);
//			ResultSet result = qexec.execSelect();
//			while (result.hasNext()) {
//				tds.add(result.next().get("g").asResource().getURI());
//			}
//		} catch (Exception e) {
//			throw e;
//		} finally {
//			dataset.end();
//		}
//
//		return tds;
//	}

	public static Model mergeVocabularies() {
		// TODO move to Queries
		// TODO add argument to scope the operation
		try (RDFConnection conn = Connector.getConnection()) {
			Model m = Txn.calculateRead(conn, () -> {
				String q = "CONSTRUCT { ?s ?p ?o } WHERE { GRAPH ?g { ?ontology a <http://www.w3.org/2002/07/owl#Ontology> . ?s ?p ?o } }";
				return conn.queryConstruct(q);
			});
			return m;
		}
	}

}