package de.thingweb.directory.sparql.client;

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

public class Connector {
	
	private static Dataset dataset = null;
	
	private static String endpoint = null;
	
	public static void init(String ep) {
		endpoint = ep;
	}
	
	public static void init(String db, String lucene) {
    	Dataset ds = TDBFactory.createDataset(db);

    	dataset = ds;
	}
	
	public static RDFConnection getConnection() {
		return getConnection(false);
	}
	
	public static RDFConnection getConnection(boolean withInference) {
		if (dataset != null) {
			Dataset ds = dataset;
			
			if (withInference) {
				Model m = ModelFactory.createDefaultModel();
				RDFConnection conn = RDFConnectionFactory.connect(ds);
				Txn.executeRead(conn, () -> {
					// loads copy of union graph into main memory
					Model union = dataset.getNamedModel(Queries.UNION_GRAPH_URI);
					m.add(union);
				});
	
				 // TODO include Pellet instead?
		    	Reasoner reasoner = ReasonerRegistry.getOWLMiniReasoner();
		    	InfModel inf = ModelFactory.createInfModel(reasoner, m);
		    	ds = DatasetFactory.create(inf);
			}
			
			return RDFConnectionFactory.connect(ds);
		} else if (endpoint != null) {
			return RDFConnectionFactory.connect(endpoint);
		}
		
		throw new RuntimeException("No RDF connection available");
	}
	
}
