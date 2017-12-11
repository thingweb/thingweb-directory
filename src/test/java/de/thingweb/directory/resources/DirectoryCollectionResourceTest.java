package de.thingweb.directory.resources;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

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
import de.thingweb.directory.rest.RESTResourceFactory;
import de.thingweb.directory.sparql.client.Connector;
import de.thingweb.directory.sparql.client.Queries;

public class DirectoryCollectionResourceTest extends BaseTest {
	
	@Test
	public void testExpirationFilter() throws Exception {
		DirectoryCollectionResource res = new DirectoryCollectionResource("/", DirectoryResource.factory());
		
		HashMap<String, String> parameters = new HashMap<>();
		InputStream empty = new ByteArrayInputStream(new byte[0]);

		parameters.put("lt", "1"); // 1s timeout
		res.post(parameters, empty);

		parameters.put("lt", "10"); // 10s timeout
		res.post(parameters, empty);
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		res.get(parameters, out);
		
		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(out.toByteArray());
		assertEquals("Filtering by lifetime was incorrect", 2, node.size());
		
		Thread.sleep(1500); // 1.5s sleep time (> first resource timeout, < second resource timeout)
		
		out = new ByteArrayOutputStream();
		res.get(parameters, out);
		
		mapper = new ObjectMapper();
		node = mapper.readTree(out.toByteArray());
		assertEquals("Expired resource was not filtered out", 1, node.size());
	}

}
