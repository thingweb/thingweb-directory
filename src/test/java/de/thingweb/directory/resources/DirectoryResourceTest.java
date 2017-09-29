package de.thingweb.directory.resources;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;

import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.system.Txn;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.thingweb.directory.BaseTest;
import de.thingweb.directory.ThingDirectory;
import de.thingweb.directory.ThingDirectoryTest;
import de.thingweb.directory.resources.DirectoryResource;
import de.thingweb.directory.rest.NotFoundException;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTResource;

public class DirectoryResourceTest extends BaseTest {

	@Test
	public void testDirectoryResourceStringInteger() throws RESTException {
		RESTResource res = DirectoryResource.factory().create("/", new HashMap<>());

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		res.get(new HashMap<>(), out);
		assertEquals("", 0, out.toByteArray().length);
	}

	@Test
	public void testGet() throws RESTException {
		Map<String, String> params = new HashMap<>();
		params.put("lt", "1"); // 1s timeout
		
		RESTResource res = DirectoryResource.factory().create("/", params);
		
		try {
			Thread.sleep(1500); // 1.5s sleep time (> resource timeout)
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			res.get(new HashMap<>(), out);
			fail("resource should have been deleted after timeout");
		} catch (InterruptedException e) {
			// then what?
			e.printStackTrace();
		} catch (NotFoundException e) {
			// as expected
		}
	}

	@Test
	public void testPut() throws RESTException {
		Map<String, String> params = new HashMap<>();
		params.put("lt", "1"); // 1s timeout
		
		RESTResource res = DirectoryResource.factory().create("/", params);
		
		try {
			Thread.sleep(500); // 0.5s sleep time (< resource timeout)
			res.put(new HashMap<>(), new ByteArrayInputStream(new byte[0]));
			Thread.sleep(750); // 1.25s sleep time in total (> initial resource timeout)
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			res.get(new HashMap<>(), out);
		} catch (InterruptedException e) {
			// then what?
			e.printStackTrace();
		}
	}

}
