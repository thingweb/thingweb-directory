package de.thingweb.directory.sparql.client;

import java.io.File;
import java.io.IOException;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.text.EntityDefinition;
import org.apache.jena.query.text.TextDatasetFactory;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.system.Txn;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.vocabulary.RDFS;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import de.thingweb.directory.ThingDirectory;

public class Connector {
	
	private static Dataset dataset;
	
	public static void init(String endpoint) {
		// TODO connection to remote SPARQL endpoint
	}
	
	public static void init(String db, String lucene) {
    	Dataset ds = TDBFactory.createDataset(db);

    	dataset = ds; // TODO config below
//        // Lucene configuration
//        try {
//            Directory luceneDir = FSDirectory.open(new File(lucene));
//            EntityDefinition entDef = new EntityDefinition("comment", "text", RDFS.comment);
//            // Set uid in order to remove index entries automatically
//            entDef.setUidField("uid");
//            StandardAnalyzer stAn = new StandardAnalyzer(Version.LUCENE_4_9);
//            dataset = TextDatasetFactory.createLucene(ds, luceneDir, entDef, stAn);
//        } catch (IOException e) {
//        	ThingDirectory.LOG.error("Cannot create RDF dataset", e);
//        }
	}
	
	public static RDFConnection getConnection() {
		return RDFConnectionFactory.connect(dataset);
	}
	
}
