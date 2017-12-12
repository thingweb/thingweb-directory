package de.thingweb.directory.resources;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import org.apache.jena.atlas.json.JsonBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.sparql.core.Transactional;
import org.apache.jena.system.Txn;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.thingweb.directory.BaseTest;
import de.thingweb.directory.resources.TDCollectionResource;
import de.thingweb.directory.rest.BadRequestException;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTResource;
import de.thingweb.directory.sparql.client.Connector;
import de.thingweb.directory.sparql.client.Queries;

public class TDCollectionResourceTest extends BaseTest {
	
	@Test
	public void testTDRepost() throws Exception {
		TDCollectionResource res = new TDCollectionResource();

		InputStream td = cl.getResourceAsStream("samples/fanTD.jsonld");
		res.post(new HashMap<>(), td);
		
		res = new TDCollectionResource();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		res.get(new HashMap<>(), out);
		
		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(out.toByteArray());
		assertEquals("TD already in the RDF store should have been reposted", 1, node.size());
	}
	
	@Test
	public void testIDGeneration() throws Exception {
		TDCollectionResource res = new TDCollectionResource();

		InputStream td = cl.getResourceAsStream("samples/fanTD.jsonld");
		RESTResource child = res.post(new HashMap<>(), td);
		
		assertEquals("Child resource name should be the TD @id", child.getName(), "urn%3AFan");

		td = cl.getResourceAsStream("samples/temperatureSensorTD.jsonld");
		child = res.post(new HashMap<>(), td);
		assertTrue("Child resource name should have been a random 4-byte hex number", child.getName().matches("[0123456789abcdef]{8}"));
	}

	@Test
	public void testPostMultipleTDs() throws JsonParseException, IOException, RESTException {
		TDCollectionResource res = new TDCollectionResource();
		
		InputStream td = cl.getResourceAsStream("samples/fanTD+temperatureSensorTD.jsonld");
		res.post(new HashMap<>(), td);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		res.get(new HashMap<>(), out);
		
		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(out.toByteArray());
		assertEquals("Not all TDs were registered (simultaneous registration)", 2, node.size());
	}
	
	@Test
	public void testPostDuplicateTD() throws Exception {
		TDCollectionResource res = new TDCollectionResource();
		
		InputStream td = cl.getResourceAsStream("samples/fanTD.jsonld");
		RESTResource child = res.post(new HashMap<>(), td);
		
		// duplicate
		td = cl.getResourceAsStream("samples/fanTD.jsonld");
		RESTResource duplicate = res.post(new HashMap<>(), td);
		
		assertSame("Duplicated TD not detected", duplicate.getName(), child.getName());
	}
	
	@Test
	public void testSPARQLFilter() throws Exception {
		TDCollectionResource res = new TDCollectionResource();
		
		HashMap<String, String> parameters = new HashMap<>();

		parameters.put("query", "?s ?p ?o");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		try {
			res.get(parameters, out);
			fail("Malformed SPARQL filter was not detected");
		} catch (BadRequestException e) {
			// expected behaviour
		}
		
		InputStream td = cl.getResourceAsStream("samples/fanTD.jsonld");
		res.post(parameters, td);
		td = cl.getResourceAsStream("samples/temperatureSensorTD.jsonld");
		res.post(parameters, td);
		
		String q = "?thing a <http://uri.etsi.org/m2m/saref#Sensor> .\n"
				+ "NOT EXISTS {"
				+ "  ?thing <http://iot.linkeddata.es/def/wot#providesInteractionPattern> ?i .\n"
				+ "  ?i a <http://uri.etsi.org/m2m/saref#ToggleCommand> .\n"
				+ "}";
		
		parameters.put("query", q);
		out = new ByteArrayOutputStream();
		res.get(parameters, out);
		
		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(out.toByteArray());
		assertEquals("SPARQL filter was not applied", 1, node.size());
	}

}
