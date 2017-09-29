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
	
	protected static ThingDirectory directory = ThingDirectory.get();
	
	protected final ClassLoader cl = ThingDirectory.get().getClass().getClassLoader();
	
	@BeforeClass
	public static void setUpRDFStore() throws Exception {
		Connector.init(DB_LOCATION, SEARCH_INDEX_LOCATION);
	}

	@AfterClass
	public static void destroyRDFStore() throws Exception {
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
