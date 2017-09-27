package de.thingweb.repository.rest;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;

import org.apache.jena.reasoner.rulesys.builtins.AssertDisjointPairs;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.thingweb.directory.rest.CollectionResource;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTResource;

public class CollectionResourceTest {

	@Test
	public void testPost() throws JsonProcessingException, IOException {
		CollectionResource coll = new CollectionResource("/", RESTResource.factory());
		ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
		
		coll.post(new HashMap<>(), in);
		coll.post(new HashMap<>(), in);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		coll.get(new HashMap<>(), out);

		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(out.toByteArray());
		assertTrue("Unexpected JSON representation (generic resource collection)", node.isArray());
		assertEquals("The collection does not have the expected children", 2, node.size());
	}

	@Test
	public void testGenerateChildID() throws Exception {
		CollectionResource coll = new CollectionResource("/", RESTResource.factory());
		ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
		
		RESTResource res1 = coll.post(new HashMap<>(), in);
		RESTResource res2 = coll.post(new HashMap<>(), in);
		
		assertNotSame("Generated resource IDs collide", res1.getName(), res2.getName());
	}

}
