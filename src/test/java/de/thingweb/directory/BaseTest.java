package de.thingweb.directory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.system.Txn;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
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
	
	protected static final String BASE_URI = "http://example.org";
	
	protected static ThingDirectory directory;
	
	protected final ClassLoader cl = ThingDirectory.get().getClass().getClassLoader();
	
	@BeforeClass
	public static void setUpRDFStore() throws Exception {
		// Note: ThingDirectory needs a proper connection to the RDF store to retrieve existing resources
		// Connector thus must be initialized before getting the ThingDirectory singleton
		Connector.init();
		directory = ThingDirectory.get();
	}

	@AfterClass
	public static void destroyRDFStore() throws Exception {
		// nothing to do
	}

	@Before
	public void cleanRDFStore() throws Exception {
		RepositoryConnection conn = Connector.getRepositoryConnection();
		conn.clear(); // delete all content in repository
	}

	protected byte[] loadResource(String location) throws IOException {
		InputStream in = cl.getResourceAsStream(location);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		byte[] buf = new byte[1024];
		int len;

		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();

		return out.toByteArray();
	}

}
