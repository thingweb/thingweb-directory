package de.thingweb.repository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.text.EntityDefinition;
import org.apache.jena.query.text.TextDatasetFactory;
import org.apache.jena.tdb.TDB;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.vocabulary.RDFS;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.flexible.standard.parser.ParseException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.eclipse.californium.core.CaliforniumLogger;

import de.thingweb.repository.coap.CoAPServer;
import de.thingweb.repository.handlers.OpenAPISpecHandler;
import de.thingweb.repository.handlers.TDLookUpEPHandler;
import de.thingweb.repository.handlers.TDLookUpHandler;
import de.thingweb.repository.handlers.TDLookUpSEMHandler;
import de.thingweb.repository.handlers.ThingDescriptionCollectionHandler;
import de.thingweb.repository.handlers.ThingDescriptionHandler;
import de.thingweb.repository.handlers.VocabularyCollectionHandler;
import de.thingweb.repository.handlers.VocabularyHandler;
import de.thingweb.repository.handlers.WelcomePageHandler;
import de.thingweb.repository.http.HTTPServer;
import de.thingweb.repository.rest.RESTException;
import de.thingweb.repository.rest.RESTHandler;
import de.thingweb.repository.rest.RESTServerInstance;

public class Repository {
	
	public static final Logger LOG = Logger.getRootLogger();

    // TODO make it private
    public Dataset dataset;
    public String baseURI;
    public static List<RESTServerInstance> servers;
    public PriorityQueue<ThingDescription> tdQueue;
    public Timer timer;
    
    private static Repository singleton;
    
    public static Repository get() {
        if (singleton == null) {
            singleton = new Repository();
        }
        return singleton;
    }
    
    public void init(String db, String uri, String lucene) {

    	Dataset ds = TDBFactory.createDataset(db);
        
        // Lucene configuration
        try {
            Directory luceneDir = FSDirectory.open(new File(lucene));
            EntityDefinition entDef = new EntityDefinition("comment", "text", RDFS.comment);
            // Set uid in order to remove index entries automatically
            entDef.setUidField("uid");
            StandardAnalyzer stAn = new StandardAnalyzer(Version.LUCENE_4_9);
            dataset = TextDatasetFactory.createLucene(ds, luceneDir, entDef, stAn);
            
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        baseURI = uri;
        servers = new ArrayList<>();
        tdQueue = new PriorityQueue<ThingDescription>();
        loadTDQueue();
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
    
    private static Set<String> listVocabularies() {
        Set<String> vocabs = new HashSet<>();

        for (String uri : VocabularyUtils.listVocabularies()) {
            vocabs.add(uri.substring(uri.lastIndexOf("/") + 1));
        }

        return vocabs;
    }
    
    private void loadTDQueue() {
    	
    	ThingDescription td;
    	for (Entry<String, String> pair : ThingDescriptionUtils.listThingDescriptionsLifetime()) {
    		td = new ThingDescription(pair.getKey(), pair.getValue());
    		tdQueue.add(td);
    	}
    	setTimer();
    }
    
    /**
     * Updates the timer with the lifetime of the
     * TD in the head of tdQueue.
     * 
     * @param newTime New delay of the timer in milliseconds.
     */
    public void setTimer(long newTime) {
    	
    	Calendar current = Calendar.getInstance();
    	long dif = newTime - current.getTimeInMillis();
    	// Must do this because the delay can't be negative
    	if ( dif <= 0) { // time already expired
    		newTime = 1;
    	} else {
    		newTime = dif;
    	}
    	
    	if (timer != null) {
    		timer.cancel();
    	}
    	
    	timer = new Timer();
    	timer.schedule(new TimerTask() {
    		@Override
    		public void run() {
    			
    			// Remove TD from queue and database
    			ThingDescription td = tdQueue.poll();
    			String uri = td.getId();
    			String id = uri.substring(uri.lastIndexOf("/") + 1);
    			ThingDescriptionHandler h = new ThingDescriptionHandler(id, servers);
    			try {
					h.delete(URI.create(uri), null, null);
				} catch (RESTException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}
    	}, newTime); // in milliseconds
    	
    }
    
    /**
     * Called every time tdQueue is updated.
     */
    public void setTimer() {
    	
    	// Set timer to head of queue if exists
    	ThingDescription t = tdQueue.peek();
    	if (t != null) {
    		setTimer(t.getLifetime().getTimeInMillis());
    	}
    }
    
    public static void main(String[] args) throws Exception {

    	// Default values
        int portCoAP = 5683;
        int portHTTP = 8080;
        String loc = "db"; // directory to store the database //"jena-config.ttl";
        String lucene = "Lucene"; // directory to store lucene index
        
        //####### Handle input ##########
        Options options = new Options();
        
        options.addOption("d", true, "Directory to store the database. Default is ./db.");
        options.addOption("l", true, "Directory to store the lucene index. Default is ./Lucene.");
        options.addOption("c", true, "CoAP port number. Default is 5683.");
        options.addOption("h", true, "HTTP port number. Default is 8080.");
        options.addOption("help", false, "This help message.");
        
        // Parse command line
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        	
        if (cmd.hasOption("c")) {
           	portCoAP = Integer.parseInt(cmd.getOptionValue("c"));
        }
        if (cmd.hasOption("h")) {
        	portHTTP = Integer.parseInt(cmd.getOptionValue("h"));
        }
        if (cmd.hasOption("d")) {
        	loc = cmd.getOptionValue("d");
        }
        if (cmd.hasOption("l")) {
        	lucene = cmd.getOptionValue("l");
        }
        if (cmd.hasOption("help")) {
        	// Automatically generate help statement
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "thingweb-repository-version.jar", options );
        	System.exit(0);
        }
        
        // ##############################

        // TODO get http URI
        Repository.get().init(loc, "http://www.example.com", lucene);
        
        RESTHandler root = new WelcomePageHandler(servers); // FIXME circular reference here...
        servers.add(new CoAPServer(portCoAP, root));
        servers.add(new HTTPServer(portHTTP, root));

        for (RESTServerInstance i : servers) {
            i.add("/" + OpenAPISpecHandler.FILENAME, new OpenAPISpecHandler(servers));
            
            i.add("/td-lookup", new TDLookUpHandler(servers));
            i.add("/td-lookup/ep", new TDLookUpEPHandler(servers));
            i.add("/td-lookup/sem", new TDLookUpSEMHandler(servers));
            
            i.add("/td", new ThingDescriptionCollectionHandler(servers));
            for (String td : listThingDescriptions()) {
                i.add("/td/" + td, new ThingDescriptionHandler(td, servers));
            }
            
            i.add("/vocab", new VocabularyCollectionHandler(servers));
            for (String vocab : listVocabularies()) {
                i.add("/vocab/" + vocab, new VocabularyHandler(vocab, servers));
            }
      
            i.start();
        }
        
        // Load ontology if it is not already there
        // TODO move to /onto and load as a vocabulary
        String fileName = "samples/qu-rec20.ttl";
        InputStream in = Repository.get().getClass().getClassLoader().getResourceAsStream(fileName);
        ThingDescriptionUtils.loadOntology(in);
        
    
        for (RESTServerInstance i : servers) {
            i.join();
        }
        Repository.get().terminate();
    }
    
}