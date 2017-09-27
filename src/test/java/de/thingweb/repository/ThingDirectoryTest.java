package de.thingweb.repository;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.json.JsonString;
import org.apache.jena.atlas.json.JsonValue;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.util.ModelUtils;
import org.apache.jena.util.FileUtils;
import org.apache.solr.client.solrj.request.CollectionAdminRequest.DeleteAlias;

import de.thingweb.directory.ThingDescription;
import de.thingweb.directory.ThingDirectory;
import de.thingweb.directory.ThingDescriptionUtils;
import de.thingweb.directory.VocabularyUtils;
import de.thingweb.directory.coap.CoAPServer;
import de.thingweb.directory.http.HTTPServer;
import de.thingweb.directory.rest.RESTHandler;
import de.thingweb.directory.rest.RESTResource;
import de.thingweb.directory.rest.RESTServerInstance;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Assert;

public class ThingDirectoryTest {
	
	/**
	 * configuration common to all unit tests
	 */
	
	public final static int COAP_PORT = 5683;
	public final static int HTTP_PORT = 8080;
	public final static String DB_LOCATION  = "DB-test";
	public final static String SEARCH_INDEX_LOCATION = "Lucene-test";
	public final static String BASE_URI = "http://www.example.com";

//	private static ThingDescriptionCollectionHandler tdch;
//	private static VocabularyCollectionHandler vch;
//
//	@BeforeClass
//	public static void oneTimeSetUp() {
//		
//		// Setup repository
//		ThingDirectory.get().init(DB_LOCATION, BASE_URI, SEARCH_INDEX_LOCATION);
//		
//		List<RESTServerInstance> servers = new ArrayList<>();
//		RESTHandler root = new WelcomePageHandler(servers);
//		servers.add(new CoAPServer(COAP_PORT, root));
//        servers.add(new HTTPServer(HTTP_PORT, root));
//
//        for (RESTServerInstance i : servers) {
//            i.add("/td-lookup", new TDLookUpHandler(servers));
//            i.add("/td-lookup/ep", new TDLookUpEPHandler(servers));
//            i.add("/td-lookup/sem", new TDLookUpSEMHandler(servers));
//            i.add("/td", new ThingDescriptionCollectionHandler(servers));
//            i.add("/vocab", new VocabularyCollectionHandler(servers));
//            i.start();
//        }
//        
//        ThingDirectory.get();
//		ThingDirectory.servers = servers;
//		
//		tdch = new ThingDescriptionCollectionHandler(servers);
//		vch = new VocabularyCollectionHandler(servers);
//
//	}
//
//	@AfterClass
//	public static void oneTimeTearDown() {
//		// Close dataset
//		Dataset ds = ThingDirectory.get().dataset;
//		ds.close();
//		
//		deleteAll(DB_LOCATION); // FIXME returns false?
//		deleteAll(SEARCH_INDEX_LOCATION);
//	}
//	
//	@Before
//	public void cleanStore() {
//		Dataset ds = ThingDirectory.get().dataset;
//		ds.begin(ReadWrite.WRITE);
//		ds.asDatasetGraph().deleteAny(Node.ANY, Node.ANY, Node.ANY, Node.ANY);
//		ds.commit();
//		ds.end();
//	}
//
//	
//	@Test
//	public void testTDManagement() throws IOException, URISyntaxException {
//		
//		RESTResource resource;
//		byte[] content;
//		String tdId, tdId2, td;
//
//		Map<String,String> parameters = new HashMap<String,String>();
//		parameters.put("ep", BASE_URI);
//		
//		// POST TD fan
//		String tdUri = "coap:///www.example.com:5686/Fan";
//		InputStream in = ThingDirectory.get().getClass().getClassLoader().getResourceAsStream("samples/fanTD.jsonld");
//		resource = tdch.post(new URI(BASE_URI + "/td"), parameters, in);
//		tdId = resource.path;
//		
//		td = ThingDescriptionUtils.getThingDescriptionIdFromUri(tdUri);
//		Assert.assertEquals("TD fan not registered", BASE_URI + tdId, td);
//		
//		
//		// POST TD temperatureSensor
//		String tdUri2 = "coap:///www.example.com:5687/temp";
//		in = ThingDirectory.get().getClass().getClassLoader().getResourceAsStream("samples/temperatureSensorTD.jsonld");
//		resource = tdch.post(new URI(BASE_URI + "/td"), parameters, in);
//		tdId2 = resource.path;
//		
//		td = ThingDescriptionUtils.getThingDescriptionIdFromUri(tdUri2);
//		Assert.assertEquals("TD temperatureSensor not registered", BASE_URI + tdId2, td);
//		
//		
//		// LOOKUP
//		Set<String> tdIds;
//		JsonObject fanQR;
//		
//		// GET by sparql query
//		parameters.clear();
//		parameters.put("query", "?s ?p ?o");
//		resource = tdch.get(new URI(BASE_URI + "/td"), parameters);
//		
//		fanQR = JSON.parse(resource.content);
//		tdIds = fanQR.keys();
//		Assert.assertFalse("TD fan not found", tdIds.isEmpty());
//		//Assert.assertEquals("Found more than one TD", 1, tdIds.size());
//		Assert.assertTrue("TD fan not found", tdIds.contains(tdId));
//		
//		/* TODO update
//		// GET by text query
//		parameters.clear();
//		parameters.put("text", "\"name AND fan\"");
//		resource = tdch.get(new URI(baseUri + "/td"), parameters);
//		
//		fanQR = JSON.parse(resource.content);
//		tdIds = fanQR.keys();
//		Assert.assertFalse("TD fan not found", tdIds.isEmpty());
//		Assert.assertTrue("TD fan not found", tdIds.contains(tdId));
//		Assert.assertFalse("TD temperatureSensor found", tdIds.contains(tdId2));
//		*/
//		
//		
//		// GET TD by id
//		ThingDescriptionHandler tdh = new ThingDescriptionHandler(tdId, ThingDirectory.get().servers);
//		resource = tdh.get(new URI(BASE_URI + tdId), null);
//		JsonObject o = JSON.parse(resource.content);
//		JsonValue v = o.get("base");
//		Assert.assertEquals("TD fan not found", "\"" + tdUri + "\"", v.toString());
//		
//		
//		// PUT TD change fan's name
//		in = ThingDirectory.get().getClass().getClassLoader().getResourceAsStream("samples/fanTD_update.jsonld");
//		content = IOUtils.toByteArray(in);
//		tdh.put(new URI(BASE_URI + tdId), new HashMap<String,String>(), new ByteArrayInputStream(content));
//			
//		// GET TD by id and check change
//		RESTResource resource2 = tdh.get(new URI(BASE_URI + tdId), null);
//		JsonObject o2 = JSON.parse(resource2.content);
//		JsonValue v2 = o2.get("name");
//		Assert.assertEquals("TD fan not updated", "\"Fan2\"", v2.toString());
//
//		
//		// DELETE TDs
//		tdh.delete(new URI(BASE_URI + tdId), null, null);
//		td = ThingDescriptionUtils.getThingDescriptionIdFromUri(tdUri);
//		Assert.assertEquals("TD fan not deleted", "NOT FOUND", td);
//		
//		tdh.delete(new URI(BASE_URI + tdId2), null, null);
//		td = ThingDescriptionUtils.getThingDescriptionIdFromUri(tdUri2);
//		Assert.assertEquals("TD temperatureSensor not deleted", "NOT FOUND", td);
//		
//	}
//	
//	@Test
//	public void testMultipleTDManagement() throws Exception {
//		Model m = ModelFactory.createDefaultModel();
//		RESTResource resource;
//		
//		Map<String,String> parameters = new HashMap<String,String>();
//		parameters.put("ep", BASE_URI);
//		
//		// POST TD fan
//		String tdFirstUri = "coap:///www.example.com:5686/Fan";
//		String tdSecondUri = "coap:///www.example.com:5687/temp";
//		InputStream in = ThingDirectory.get().getClass().getClassLoader().getResourceAsStream("samples/fanTD+temperatureSensorTD.jsonld");
//	
//		tdch.post(new URI(BASE_URI + "/td"), parameters, in);
//		
//		List<String> uris = ThingDescriptionUtils.listThingDescriptionsUri();
//		Assert.assertEquals("TD fan and TD temperatureSensor not registered as expected", 2, uris.size());
//		
//		// note: it is unknown whether resource path is fan TD's or Sensor TD's
//	}
//	
//	@Test
//	public void testDuplicateDetection() throws Exception {		
//		Map<String,String> parameters = new HashMap<String,String>();
//		parameters.put("ep", BASE_URI);
//		
//		// POST TD fan
//		InputStream in = ThingDirectory.get().getClass().getClassLoader().getResourceAsStream("samples/fanTD.jsonld");
//		String buf = ThingDescriptionUtils.streamToString(in);
//
//		RESTResource id1 = tdch.post(new URI(BASE_URI + "/td"), parameters, new ByteArrayInputStream(buf.getBytes()));
//		RESTResource id2 = tdch.post(new URI(BASE_URI + "/td"), parameters, new ByteArrayInputStream(buf.getBytes()));
//		
//		Assert.assertTrue("TD duplicates not detected", id1.name.equals(id2.name));
//	}
//	
//	@Test
//	public void testTDContentNegotiation() throws Exception {
//		Model m = ModelFactory.createDefaultModel();
//		RESTResource resource;
//		
//		Map<String,String> parameters = new HashMap<String,String>();
//		parameters.put("ep", BASE_URI);
//		parameters.put("ct", "text/turtle");
//		
//		// POST TD fan
//		String tdUri = "coap:///www.example.com:5686/Fan";
//		InputStream in = ThingDirectory.get().getClass().getClassLoader().getResourceAsStream("samples/fanTD.jsonld");
//		m.read(in, tdUri, "JSON-LD");
//		
//		ByteArrayOutputStream out = new ByteArrayOutputStream();
//		m.write(out, "Turtle");
//		in = new ByteArrayInputStream(out.toByteArray());
//	
//		resource = tdch.post(new URI(BASE_URI + "/td"), parameters, in);
//		String tdId = resource.path;
//		
//		String id = ThingDescriptionUtils.getThingDescriptionIdFromUri(tdUri);
//		Assert.assertEquals("TD fan not registered (Turtle format)", BASE_URI + tdId, id);
//	}
//	
//	@Test
//	public void testVocabularyManagement() throws Exception {
//		RESTResource resource;
//		String ontoId;
//
//		Map<String,String> parameters = new HashMap<String,String>();
//		
//		// POST vocabulary
//		String sosaUri = "http://www.w3.org/ns/sosa/";
//		InputStream in = ThingDirectory.get().getClass().getClassLoader().getResourceAsStream("onto/sosa.ttl");
//		resource = vch.post(new URI(BASE_URI + "/vocab"), parameters, in);
//		ontoId = resource.path;
//
//		Assert.assertTrue("SOSA ontology not registered", VocabularyUtils.containsVocabulary(sosaUri));
//		
//		// GET vocabulary by SPARQL query
//		parameters.clear();
//		parameters.put("query", "?s ?p ?o");
//		resource = vch.get(new URI(BASE_URI + "/vocab"), parameters);
//		
//		JsonValue ontoIds = JSON.parseAny(resource.content);
//		Assert.assertTrue("Vocabulary collection is not an array", ontoIds.isArray());
//		Assert.assertTrue("SOSA ontology not found", ontoIds.getAsArray().contains(new JsonString(ontoId)));
//		
//		// GET vocabulary by id
//		VocabularyHandler vh = new VocabularyHandler(ontoId, ThingDirectory.servers);
//		resource = vh.get(new URI(BASE_URI + ontoId), null);
//		
//		ByteArrayInputStream byteStream = new ByteArrayInputStream(resource.content.getBytes());
//		Model m = ModelFactory.createDefaultModel();
//		m.read(byteStream, "", "Turtle");
//		Assert.assertFalse("SOSA ontology definition is not valid", m.isEmpty());
//
//		// DELETE vocabulary
//		vh.delete(new URI(BASE_URI + ontoId), null, null);
//		Assert.assertFalse("SOSA ontology not deleted", VocabularyUtils.containsVocabulary(sosaUri));
//	}
//	
//	@Test
//	public void testVocabularyContentNegotiation() throws Exception {
//		Model m = ModelFactory.createDefaultModel();
//		
//		Map<String,String> parameters = new HashMap<String,String>();
//		parameters.put("ep", BASE_URI);
//		parameters.put("ct", "application/ld+json");
//		
//		// POST vocabulary
//		String sosaUri = "http://www.w3.org/ns/sosa/";
//		InputStream in = ThingDirectory.get().getClass().getClassLoader().getResourceAsStream("onto/sosa.ttl");
//		m.read(in, sosaUri, "Turtle");
//		
//		ByteArrayOutputStream out = new ByteArrayOutputStream();
//		m.write(out, "JSON-LD");
//		in = new ByteArrayInputStream(out.toByteArray());
//	
//		vch.post(new URI(BASE_URI + "/vocab"), parameters, in);
//		
//		Assert.assertTrue("SOSA ontology not found (JSON-LD format)", VocabularyUtils.containsVocabulary(sosaUri));
//	}
//	
//	// ***** EXTRAS *****
//	
//	
//	/**
//	 * Returns the content of a TD json-ld file.
//	 * Mocks the behavior of doing a GET to the TD's uri.
//	 * 
//	 * @param filePath Path of the json-ld file.
//	 * @return Content of the file in a String.
//	 * @throws IOException
//	 */
//	private byte[] getThingDescription(URI filePath) throws IOException {
//		
//		return Files.readAllBytes(Paths.get(filePath));
//	}
//	
//	private static boolean deleteAll(String dirPath) {
//		File dir = new File(dirPath);
//		for (File f : dir.listFiles()) {
//			f.delete();
//		}
//		return dir.delete();
//	}

}
