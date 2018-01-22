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

public class TDLookUpSemResourceTest extends BaseTest {
	
	@Test
	public void testSPARQLFilter() throws Exception {
		TDCollectionResource td = new TDCollectionResource();
		TDLookUpSemResource sem = new TDLookUpSemResource(td);
		
		HashMap<String, String> parameters = new HashMap<>();
		
		InputStream in = cl.getResourceAsStream("samples/fanTD.jsonld");
		td.post(parameters, in);
		in = cl.getResourceAsStream("samples/temperatureSensorTD.jsonld");
		td.post(parameters, in);
		
		String q = "?thing a <http://uri.etsi.org/m2m/saref#Sensor> .\n"
				+ "FILTER NOT EXISTS {"
				+ "  ?thing <http://iot.linkeddata.es/def/wot#providesInteractionPattern> ?i .\n"
				+ "  ?i a <http://uri.etsi.org/m2m/saref#ToggleCommand> .\n"
				+ "}";
		
		parameters.put("query", q);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		sem.get(parameters, out);
		
		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(out.toByteArray());
		assertEquals("SPARQL filter was not applied", 1, node.size());
	}

}
