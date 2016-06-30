package de.thingweb.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.Dataset;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.query.DatasetFactory;

import de.thingweb.repository.coap.CoAPServer;
import de.thingweb.repository.http.HTTPServer;
import de.thingweb.repository.rest.RESTServerInstance;

public class Repository {
  
	// TODO make it private
	public Dataset dataset;
	public String baseURI;
	
	private static Repository singleton;
	
	public static Repository get() {
		if (singleton == null) {
			singleton = new Repository();
		}
		return singleton;
	}
	
	private void init(String db, String uri) {
       //dataset = TDBFactory.createDataset(db);
	  // Use the assembler file
	  dataset = DatasetFactory.assemble("jena-config.ttl", "http://localhost/jena_example/#text_dataset");
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
	  int portCoAP = 5685;
	  int portHTTP = 8080;
    String loc = "database";
    
    if (args.length >= 1) {
      loc = args[0];
    }
    if (args.length >= 2) {
      portCoAP = Integer.parseInt(args[1]);
    }
    if (args.length >= 3) {
      portHTTP = Integer.parseInt(args[2]);
    }
    
    // TODO get http URI
    Repository.get().init(loc, "http://www.example.com");
    
    List<RESTServerInstance> servers = new ArrayList<>();
    servers.add(new CoAPServer(portCoAP));
    servers.add(new HTTPServer(portHTTP));

    for (RESTServerInstance i : servers) {
    	i.add("/td-lookup", new TDLookUpHandler(servers));
      	i.add("/td-lookup/ep", new TDLookUpEPHandler(servers));
      	i.add("/td-lookup/sparql", new TDLookUpSparqlHandler(servers));
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
