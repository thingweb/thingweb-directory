package de.thingweb.repository;

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

public class BaseTest {
	
	static ThingDirectory directory = ThingDirectory.get();
	
	protected final ClassLoader cl = ThingDirectory.get().getClass().getClassLoader();
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		directory.init(ThingDirectoryTest.DB_LOCATION,
				ThingDirectoryTest.BASE_URI,
				ThingDirectoryTest.SEARCH_INDEX_LOCATION);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		// TODO delete DB and Lucene folders
	}

	@Before
	public void setUp() throws Exception {
		try (RDFConnection conn = directory.getStoreConnection()) {
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

	@After
	public void tearDown() throws Exception {
	}

}
