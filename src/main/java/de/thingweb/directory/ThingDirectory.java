package de.thingweb.directory;

import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;

import de.thingweb.directory.coap.CoAPServer;
import de.thingweb.directory.http.HTTPServer;
import de.thingweb.directory.resources.WelcomePageResource;
import de.thingweb.directory.rest.IndexResource;
import de.thingweb.directory.rest.RESTServerInstance;
import de.thingweb.directory.sparql.client.Connector;
import de.thingweb.directory.sparql.server.Functions;

@SwaggerDefinition(tags = {
	@Tag(name = "thing_description",
         description = "WoT Thing Description management interface"),
    @Tag(name = "vocabulary",
         description = "Vocabulary (OWL Ontology) management interface")
                   }, info =
    @Info(version = "0.7",
          title = "thingweb-directory",
          description = "W3C WoT Thing Directory implementation. Also available over CoAP.",
          contact = @Contact(name = "Victor Charpenay",
                             email = "victor.charpenay@siemens.com"),
          license = @License(name = "MIT",
                             url = "https://spdx.org/licenses/MIT.html")))
public class ThingDirectory {
	
	public static final Logger LOG = Logger.getRootLogger();
	
	public static final int DEFAULT_COAP_PORT = 5683;
	public static final int DEFAULT_HTTP_PORT = 8080;
    
	// TODO fix addressing
    private final static String baseURI = "http://localhost";

    private final Set<RESTServerInstance> servers = new HashSet<>();
    
    private final IndexResource index = new WelcomePageResource();
    
    private static ThingDirectory singleton = null;
    
    public static String getBaseURI() {
    	return baseURI;
    }
    
    public static ThingDirectory get() {
    	if (singleton == null) {
    		singleton = new ThingDirectory();
    	}
        return singleton;
    }
    
    private ThingDirectory() {
    	// constructor is private and should only be called once
    }
    
    public Set<RESTServerInstance> getInstances() {
    	return servers;
    }
    
    public void run(Collection<RESTServerInstance> instances) {       
        servers.addAll(instances);

        for (RESTServerInstance s : servers) {
        	s.setIndex(index);
        	s.start();
        }

//        for (RESTServerInstance i : servers) {
//            i.join();
//        }
        
        ThingDirectory.get().terminate();
    }
    
    public void terminate() {
        // TODO anything to do?
    }

    // TODO periodically remove resources whose lifetime expired
    
    public static void main(String[] args) throws Exception {

    	// Default values
        int portCoAP = DEFAULT_COAP_PORT;
        int portHTTP = DEFAULT_HTTP_PORT;
        String queryEndpoint = null; // SPARQL query endpoint
        String updateEndpoint = null; // SPARQL update endpoint
        
        //####### Handle input ##########
        Options options = new Options();
        
        options.addOption("q", true, "SPARQL query endpoint URI for remote storage (main memory storage if not provided).");
        options.addOption("u", true, "SPARQL update endpoint URI for remote storage (same as query endpoint if not provided).");
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
        if (cmd.hasOption("q") && cmd.hasOption("u")) {
        	queryEndpoint = cmd.getOptionValue("q");
        	if (cmd.hasOption("u")) {
        		updateEndpoint = cmd.getOptionValue("u");
        	} else {
        		updateEndpoint = queryEndpoint;
        	}
        }
        if (cmd.hasOption("help")) {
        	// Automatically generate help statement
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "thingweb-repository-version.jar", options );
        	System.exit(0);
        }
        
        // ##############################

        // initiate SPARQL client
        if (queryEndpoint != null) {
            Connector.init(queryEndpoint, updateEndpoint);
            // TODO fall back to embedded store if endpoint not responding
        } else {
            Connector.init();
        }

        // configure SPARQL engine (server)
        Functions.registerAll();
        
        // create and start REST server instances
        Set<RESTServerInstance> servers = new HashSet<>();
        servers.add(new CoAPServer(portCoAP));
        servers.add(new HTTPServer(portHTTP));
        
        ThingDirectory.get().run(servers);
    }
    
}