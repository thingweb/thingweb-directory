package de.thingweb.directory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.system.Txn;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;






import de.thingweb.directory.ThingDirectory;
import de.thingweb.directory.sparql.client.Connector;

/**
 * configuration common to all unit tests
 */
public class BaseTest {
	
	public final static String DB_LOCATION  = "DB-test";
	public final static String SEARCH_INDEX_LOCATION = "Lucene-test";
	
	protected static ThingDirectory directory;
	
	protected final ClassLoader cl = ThingDirectory.get().getClass().getClassLoader();
	
	@BeforeClass
	public static void setUpRDFStore() throws Exception {
		// Note: ThingDirectory needs a proper connection to the RDF store to retrieve existing resources
		// Connector thus must be initialized before getting the ThingDirectory singleton
		Connector.init(DB_LOCATION, SEARCH_INDEX_LOCATION);
		directory = ThingDirectory.get();
	}

	@AfterClass
	public static void destroyRDFStore() throws Exception {
		// "On Microsoft Windows, "mapped" databases can not be deleted while the JVM is running on MS Windows. This is a known issue with Java."
		// http://jena.apache.org/documentation/tdb/store-parameters.html
		deleteAll(DB_LOCATION); // FIXME returns false?
//		deleteAll(SEARCH_INDEX_LOCATION);
	}

	@Before
	public void cleanRDFStore() throws Exception {
		try (RDFConnection conn = Connector.getConnection()) {
			Txn.executeWrite(conn, () -> {
				conn.delete();
				List<String> uris = new ArrayList<>();
				conn.querySelect("SELECT ?g WHERE { GRAPH ?g { ?s ?p ?o } }", (r) -> {
					uris.add(r.get("g").asResource().getURI());
				});
				uris.forEach((uri) -> {
					conn.delete(uri);
				}); 
			});
		}
	}
	
	private static boolean deleteAll(String dirPath) {
		File dir = new File(dirPath);
		
		for (File f : dir.listFiles()) {
			f.delete();
		}
		
		return dir.delete();
	}

}
