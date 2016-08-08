package de.thingweb.repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.text.EntityDefinition;
import org.apache.jena.query.text.TextDatasetFactory;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.californium.core.coap.OptionNumberRegistry.optionFormats;
import org.eclipse.californium.core.server.resources.Resource;

import de.thingweb.repository.coap.CoAPServer;
import de.thingweb.repository.handlers.TDLookUpEPHandler;
import de.thingweb.repository.handlers.TDLookUpHandler;
import de.thingweb.repository.handlers.TDLookUpSEMHandler;
import de.thingweb.repository.http.HTTPServer;
import de.thingweb.repository.rest.RESTHandler;
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
    
    private void init(String db, String uri, String lucene) {
        Dataset ds = TDBFactory.createDataset(db);
        baseURI = uri;
        
        // Lucene configuration
        try {
            Directory luceneDir = FSDirectory.open(new File(lucene));
            EntityDefinition entDef = new EntityDefinition("comment", "text", RDFS.comment);
            StandardAnalyzer stAn = new StandardAnalyzer(Version.LUCENE_4_9);
            dataset = TextDatasetFactory.createLucene(ds, luceneDir, entDef, stAn);
            
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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
        int portCoAP = 5683;
        int portHTTP = 8080;
        String loc = "db"; // directory to store the database //"jena-config.ttl";
        String lucene = "Lucene"; // directory to store lucene index

        if (args.length >= 1) {
            loc = args[0];
        }
        if (args.length >= 2) {
            portCoAP = Integer.parseInt(args[1]);
        }
        if (args.length >= 3) {
            portHTTP = Integer.parseInt(args[2]);
        }
        if (args.length >= 4) {
            lucene = args[3];
        }
    
        // TODO get http URI
        Repository.get().init(loc, "http://www.example.com", lucene);

        List<RESTServerInstance> servers = new ArrayList<>();
        RESTHandler root = new WelcomePageHandler(servers); // FIXME circular reference here...
        servers.add(new CoAPServer(portCoAP, root));
        servers.add(new HTTPServer(portHTTP, root));

        for (RESTServerInstance i : servers) {
            i.add("/td-lookup", new TDLookUpHandler(servers));
            i.add("/td-lookup/ep", new TDLookUpEPHandler(servers));
            i.add("/td-lookup/sem", new TDLookUpSEMHandler(servers));
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
