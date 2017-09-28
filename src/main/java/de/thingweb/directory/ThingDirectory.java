package de.thingweb.directory;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.log4j.Logger;

import de.thingweb.directory.coap.CoAPServer;
import de.thingweb.directory.http.HTTPServer;
import de.thingweb.directory.resources.WelcomePageResource;
import de.thingweb.directory.rest.RESTServerInstance;
import de.thingweb.directory.sparql.client.Connector;
import de.thingweb.directory.sparql.server.Functions;

public class ThingDirectory {
	
	public static final Logger LOG = Logger.getRootLogger();
    
	// TODO get HTTP URI?
    private String baseURI = "http://localhost";

    private Set<RESTServerInstance> servers = new HashSet<>();
    
    private static ThingDirectory singleton;
    
    public static ThingDirectory get() {
        if (singleton == null) {
            singleton = new ThingDirectory();
        }
        return singleton;
    }
    
    private ThingDirectory() {
    	// constructor is private and should only be called once
    }
    
    public RDFConnection getStoreConnection() {
    	return Connector.getConnection();
    }
    
    public String getBaseURI() {
    	return baseURI;
    }
    
    private void terminate() {
        // TODO anything to do?
    }

    // TODO periodically remove resources whose lifetime expired
    
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

        // initiate SPARQL client
        Connector.init(loc, lucene);

        // configure SPARQL engine (server)
        Functions.registerAll();
        
        // create and start REST server instances
        ThingDirectory directory = ThingDirectory.get();
        directory.servers.add(new CoAPServer(portCoAP));
        directory.servers.add(new HTTPServer(portHTTP));

        WelcomePageResource index = new WelcomePageResource();

        for (RESTServerInstance s : directory.servers) {
        	s.setIndex(index);
        	s.start();
        }

        for (RESTServerInstance i : directory.servers) {
            i.join();
        }
        
        ThingDirectory.get().terminate();
    }
    
}