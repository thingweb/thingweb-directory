package de.thingweb.directory.rest;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.reasoner.rulesys.builtins.AssertDisjointPairs;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.thingweb.directory.rest.CollectionFilter;
import de.thingweb.directory.rest.CollectionFilterFactory;
import de.thingweb.directory.rest.CollectionResource;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTResource;

public class CollectionResourceTest {

	@Test
	public void testPost() throws JsonProcessingException, IOException, RESTException {
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
	
	@Test
	public void testCollectionFilter() throws Exception {
		CollectionFilter f = new CollectionFilter() {
			@Override
			public boolean keep(RESTResource child) {
				return false;
			}
		};
		
		CollectionResource coll = new CollectionResource("/", RESTResource.factory(), new CollectionFilterFactory() {
			@Override
			public CollectionFilter create(Map<String, String> parameters) {
				return f;
			}
		});
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		coll.get(new HashMap<>(), out);
		
		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(out.toByteArray());
		assertEquals("'keep none' filter was not applied", 0, node.size());
	}

}
