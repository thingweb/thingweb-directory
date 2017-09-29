package de.thingweb.repository.resources;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Test;

import de.thingweb.directory.resources.VocabularyCollectionResource;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTResource;
import de.thingweb.repository.BaseTest;

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

}
