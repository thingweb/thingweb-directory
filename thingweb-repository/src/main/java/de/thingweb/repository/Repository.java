package de.thingweb.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.Dataset;
import org.apache.jena.tdb.TDBFactory;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.californium.core.server.resources.Resource;

import de.thingweb.repository.coap.CoAPRESTResource;
import de.thingweb.repository.coap.CoAPServer;
import de.thingweb.repository.http.HTTPRESTResource;
import de.thingweb.repository.http.HTTPServer;
import de.thingweb.repository.rest.RESTHandler;
import de.thingweb.repository.rest.RESTServerInstance;

public class Repository {

	// TODO make it private
	public Dataset dataset;
	public String baseURI;
	
	public static Repository singleton;
	
	public static Repository get() {
		if (singleton == null) {
			singleton = new Repository();
		}
		return singleton;
	}
	
	private void init(String db, String uri) {
	  dataset = TDBFactory.createDataset(db);
	  baseURI = uri;
	}
	
	private void terminate() {
	  dataset.close();
	}
	
	private static List<String> listThingDescriptions() {
	  List<String> tds = new ArrayList<>();
	  for (String uri : ThingDescriptionUtils.listThingDescriptions("?s ?p ?o")) {
	    tds.add(uri.substring(uri.lastIndexOf("/") + 1));
    }
	  return tds;
	}
	
	public static void main(String[] args) throws Exception {
    String loc = "db";
    if (args.length >= 1) {
      loc = args[0];
    }
    // TODO get http URI
    Repository.get().init(loc, "http://www.example.com");
    
    List<RESTServerInstance> servers = new ArrayList<>();
    servers.add(new CoAPServer(5683));
    servers.add(new HTTPServer(8080));

    for (RESTServerInstance i : servers) {
      i.add("/td", new ThingDescriptionCollectionHandler(servers));
      for (String td : listThingDescriptions()) {
        i.add("/td/" + td, new ThingDescriptionHandler(td, servers));
      }
      
      i.start();
    }
    
    for (RESTServerInstance i : servers) {
      i.join();
    }
	  Repository.get().terminate();
	}
	
}
