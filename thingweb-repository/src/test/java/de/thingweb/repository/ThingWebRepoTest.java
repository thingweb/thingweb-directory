package de.thingweb.repository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.json.JsonValue;
import org.apache.jena.query.Dataset;

import de.thingweb.repository.coap.CoAPServer;
import de.thingweb.repository.handlers.TDLookUpEPHandler;
import de.thingweb.repository.handlers.TDLookUpHandler;
import de.thingweb.repository.handlers.TDLookUpSEMHandler;
import de.thingweb.repository.http.HTTPServer;
import de.thingweb.repository.rest.RESTHandler;
import de.thingweb.repository.rest.RESTResource;
import de.thingweb.repository.rest.RESTServerInstance;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Assert;

public class ThingWebRepoTest {

	private static ThingDescriptionCollectionHandler tdch;
	
	private final static int portCoap = 5683;
	private final static int portHttp = 8080;
	private final static String dbPath  = "db";
	private final static String idxPath = "Lucene";
	private static String baseUri = "http://www.example.com";

	@BeforeClass
	public static void oneTimeSetUp() {
		
		// Setup repository
		Repository.get().init(dbPath, baseUri, idxPath);
		
		List<RESTServerInstance> servers = new ArrayList<>();
		RESTHandler root = new WelcomePageHandler(servers);
		servers.add(new CoAPServer(portCoap, root));
        servers.add(new HTTPServer(portHttp, root));

        for (RESTServerInstance i : servers) {
            i.add("/td-lookup", new TDLookUpHandler(servers));
            i.add("/td-lookup/ep", new TDLookUpEPHandler(servers));
            i.add("/td-lookup/sem", new TDLookUpSEMHandler(servers));
            i.add("/td", new ThingDescriptionCollectionHandler(servers));
            i.start();
        }
        
        Repository.get();
		Repository.servers = servers;
		
		tdch = new ThingDescriptionCollectionHandler(servers);

	}

	@AfterClass
	public static void oneTimeTearDown() {
		
		// Close dataset
		Dataset ds = Repository.get().dataset;
		ds.close();
	}

	
	@Test
	public void testREST() throws IOException, URISyntaxException {
		
		RESTResource resource;
		byte[] content;
		String tdId, tdId2, td;
		
		Map<String,String> parameters = new HashMap<String,String>();
		parameters.put("ep", baseUri);
		
		// POST TD fan
		String tdUri = "coap:///www.example.com:5686/Fan";
		InputStream in = Repository.get().getClass().getClassLoader().getResourceAsStream("samples/fanTD.jsonld");
		resource = tdch.post(new URI(baseUri + "/td"), parameters, in);
		tdId = resource.path;
		
		td = ThingDescriptionUtils.getThingDescriptionIdFromUri(tdUri);
		Assert.assertEquals("TD fan not registered", baseUri + tdId, td);
		
		
		// POST TD temperatureSensor
		String tdUri2 = "coap:///www.example.com:5687/temp";
		in = Repository.get().getClass().getClassLoader().getResourceAsStream("samples/temperatureSensorTD.jsonld");
		resource = tdch.post(new URI(baseUri + "/td"), parameters, in);
		tdId2 = resource.path;
			
		td = ThingDescriptionUtils.getThingDescriptionIdFromUri(tdUri2);
		Assert.assertEquals("TD temperatureSensor not registered", baseUri + tdId2, td);
		
		
		// LOOKUP
		Set<String> tdIds;
		JsonObject fanQR;
		
		// GET by sparql query
		parameters.clear();
		parameters.put("query", "?s ?p ?o");
		resource = tdch.get(new URI(baseUri + "/td"), parameters);
		
		fanQR = JSON.parse(resource.content);
		tdIds = fanQR.keys();
		Assert.assertFalse("TD fan not found", tdIds.isEmpty());
		//Assert.assertEquals("Found more than one TD", 1, tdIds.size());
		Assert.assertTrue("TD fan not found", tdIds.contains(tdId));
		
		
		// GET by text query
		parameters.clear();
		parameters.put("text", "\"name AND fan\"");
		resource = tdch.get(new URI(baseUri + "/td"), parameters);
		
		fanQR = JSON.parse(resource.content);
		tdIds = fanQR.keys();
		Assert.assertFalse("TD fan not found", tdIds.isEmpty());
		Assert.assertTrue("TD fan not found", tdIds.contains(tdId));
		Assert.assertFalse("TD temperatureSensor found", tdIds.contains(tdId2));
		
		
		
		// GET TD by id
		ThingDescriptionHandler tdh = new ThingDescriptionHandler(tdId, Repository.get().servers);
		resource = tdh.get(new URI(baseUri + tdId), null);
		JsonObject o = JSON.parse(resource.content);
		JsonValue v = o.get("uris").getAsArray().get(0);
		Assert.assertEquals("TD fan not found", "\"" + tdUri + "\"", v.toString());
		
		
		// PUT TD change fan's name
		in = Repository.get().getClass().getClassLoader().getResourceAsStream("samples/fanTD_update.jsonld");
		content = IOUtils.toByteArray(in);
		tdh.put(new URI(baseUri + tdId), new HashMap<String,String>(), new ByteArrayInputStream(content));
			
		// GET TD by id and check change
		RESTResource resource2 = tdh.get(new URI(baseUri + tdId), null);
		JsonObject o2 = JSON.parse(resource2.content);
		JsonValue v2 = o2.get("name");
		Assert.assertEquals("TD fan not updated", "\"Fan2\"", v2.toString());

		
		// DELETE TDs
		tdh.delete(new URI(baseUri + tdId), null, null);
		td = ThingDescriptionUtils.getThingDescriptionIdFromUri(tdUri);
		Assert.assertEquals("TD fan not deleted", "NOT FOUND", td);
		
		tdh.delete(new URI(baseUri + tdId2), null, null);
		td = ThingDescriptionUtils.getThingDescriptionIdFromUri(tdUri2);
		Assert.assertEquals("TD temperatureSensor not deleted", "NOT FOUND", td);
		
	}
	
	
	
	// ***** EXTRAS *****
	
	
	/**
	 * Returns the content of a TD json-ld file.
	 * Mocks the behavior of doing a GET to the TD's uri.
	 * 
	 * @param filePath Path of the json-ld file.
	 * @return Content of the file in a String.
	 * @throws IOException
	 */
	public byte[] getThingDescription(URI filePath) throws IOException {
		
		return Files.readAllBytes(Paths.get(filePath));
	}

}
