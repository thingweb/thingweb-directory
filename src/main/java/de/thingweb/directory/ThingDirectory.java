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
    
	// TODO get HTTP URI?
    private final String baseURI = "http://localhost";

    private final Set<RESTServerInstance> servers = new HashSet<>();
    
    private final IndexResource index = new WelcomePageResource();
    
    private static ThingDirectory singleton = null;
    
    public static ThingDirectory get() {
    	if (singleton == null) {
    		singleton = new ThingDirectory();
    	}
        return singleton;
    }
    
    private ThingDirectory() {
    	// constructor is private and should only be called once
    }
    
    public String getBaseURI() {
    	return baseURI;
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
        String loc = "db"; // directory to store the database
        String lucene = "lucene"; // directory to store lucene index
        String endpoint = null; // SPARQL endpoint URI for remote storage
        
        //####### Handle input ##########
        Options options = new Options();
        
        options.addOption("d", true, "Directory to store the database. Default is ./db.");
        options.addOption("e", true, "SPARQL endpoint URI for remote storage (not compatible with -d).");
        options.addOption("l", true, "Directory to store the lucene index. Default is ./lucene.");
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
        if (cmd.hasOption("e")) {
        	endpoint = cmd.getOptionValue("e");
        } else if (cmd.hasOption("d")) {
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
        if (endpoint != null) {
            Connector.init(endpoint);       	
        } else {
            Connector.init(loc, lucene); 
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