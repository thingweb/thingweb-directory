package de.thingweb.repository.resources;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import org.apache.jena.atlas.json.JsonBuilder;
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

import de.thingweb.directory.resources.TDCollectionResource;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTResource;
import de.thingweb.repository.BaseTest;

public class TDCollectionResourceTest extends BaseTest {

	@Test
	public void testPostMultipleTDs() throws JsonParseException, IOException {
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
		
		assertNotSame("Duplicated TD not detected", duplicate.getName(), child.getName());
	}
	
	@Test
	public void testSPARQLFilter() throws Exception {
		TDCollectionResource res = new TDCollectionResource();
		
		HashMap<String, String> parameters = new HashMap<>();
		
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
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		res.get(parameters, out);
		
		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(out.toByteArray());
		assertEquals("SPARQL filter was not applied", 1, node.size());
	}

}
