package de.thingweb.directory.resources;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RiotException;
import org.junit.Test;

import de.thingweb.directory.BaseTest;
import de.thingweb.directory.rest.NotFoundException;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTResource;

public class RDFDocumentTest extends BaseTest {

	@Test
	public void testFactory() {
		InputStream in = cl.getResourceAsStream("samples/fanTD.jsonld");
		Model m = ModelFactory.createDefaultModel();
		m.read(in, "", "JSON-LD");
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		m.write(out, "Turtle");
		in = new ByteArrayInputStream(out.toByteArray());
		
		Map<String,String> parameters = new HashMap<String,String>();
		parameters.put("ct", "text/turtle");
		
		RDFDocument.factory().create("/", parameters, in);
	}

	@Test
	public void testGet() throws RESTException {
		InputStream in = cl.getResourceAsStream("samples/fanTD.jsonld");
		RESTResource resource = RDFDocument.factory().create("/", new HashMap<>(), in);
		
		Map<String,String> parameters = new HashMap<String,String>();
		parameters.put("accept", "text/turtle");

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		resource.get(parameters, out);
		in = new ByteArrayInputStream(out.toByteArray());
		
		Model m = ModelFactory.createDefaultModel();
		try {
			m.read(in, "", "Turtle");
		} catch (RiotException e) {
			fail("RDF document cannot be parsed (Turtle)");
		}
	}

	@Test
	public void testPut() throws RESTException {
		InputStream in = cl.getResourceAsStream("samples/fanTD.jsonld");
		RESTResource resource = RDFDocument.factory().create("/", new HashMap<>(), in);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		resource.get(new HashMap<>(), out);
		byte[] origin = out.toByteArray();

		in = cl.getResourceAsStream("samples/fanTD_update.jsonld");
		resource.put(new HashMap<>(), in);

		out = new ByteArrayOutputStream();
		resource.get(new HashMap<>(), out);
		byte[] updated = out.toByteArray();
		
		assertFalse("TD Fan not updated", Arrays.equals(origin, updated));
		// FIXME test RDF graph isomorphism instead
	}

	@Test
	public void testDelete() throws RESTException {
		InputStream in = cl.getResourceAsStream("samples/fanTD.jsonld");
		RESTResource resource = RDFDocument.factory().create("/", new HashMap<>(), in);
		
		resource.delete(new HashMap<>());
		
		try {
			ByteArrayOutputStream sink = new ByteArrayOutputStream();
			resource.get(new HashMap<>(), sink);
			fail("TD Fan not deleted");
		} catch (NotFoundException e) {
			// as expected
		}
	}

}
