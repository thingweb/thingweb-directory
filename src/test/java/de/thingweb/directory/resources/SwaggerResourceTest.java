package de.thingweb.directory.resources;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.thingweb.directory.resources.SwaggerResource;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTResource;

public class SwaggerResourceTest {

	@Test
	public void testGet() throws JsonProcessingException, IOException, RESTException {
		RESTResource res = new SwaggerResource();
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		res.get(new HashMap<>(), out);
		
		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(out.toByteArray());
		assertTrue("Swagger specification is not a JSON object", node.isObject());
	}

}
