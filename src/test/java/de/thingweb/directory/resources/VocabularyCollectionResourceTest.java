package de.thingweb.directory.resources;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.thingweb.directory.BaseTest;
import de.thingweb.directory.resources.VocabularyCollectionResource;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTResource;

public class VocabularyCollectionResourceTest extends BaseTest {

	@Test
	public void testPost() throws RESTException {
		VocabularyCollectionResource coll = new VocabularyCollectionResource();
		
		InputStream in = cl.getResourceAsStream("onto/sosa.ttl");
		Map<String, String> parameters = new HashMap<>();
		parameters.put("ct", "text/turtle");
		
		RESTResource vocab = coll.post(parameters, in);
		
		assertNotNull(vocab);
	}
	
	@Test
	public void testRepost() throws Exception {
		VocabularyCollectionResource coll = new VocabularyCollectionResource();

		InputStream td = cl.getResourceAsStream("onto/sosa.ttl");
		
		Map<String, String> parameters = new HashMap<>();
		parameters.put("ct", "text/turtle");
		
		coll.post(parameters, td);
		
		coll = new VocabularyCollectionResource();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		coll.get(new HashMap<>(), out);
		
		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(out.toByteArray());
		assertEquals("Vocabulary already in the RDF store should have been reposted", 1, node.size());
	}

}
