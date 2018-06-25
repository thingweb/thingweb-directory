/********************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
 * 
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the W3C Software Notice and
 * Document License (2015-05-13) which is available at
 * https://www.w3.org/Consortium/Legal/2015/copyright-software-and-document.
 * 
 * SPDX-License-Identifier: EPL-2.0 OR W3C-20150513
 ********************************************************************************/
package org.eclipse.thingweb.directory;

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
import org.eclipse.thingweb.directory.coap.CoAPServer;
import org.eclipse.thingweb.directory.http.HTTPServer;
import org.eclipse.thingweb.directory.rest.CollectionServlet;
import org.eclipse.thingweb.directory.rest.RESTServletContainer;
import org.eclipse.thingweb.directory.servlet.*;
import org.eclipse.thingweb.directory.sparql.client.Connector;

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
	public static final int DEFAULT_HTTP_PORT = 8090;
    
	// TODO fix addressing
    private final static String baseURI = "http://localhost";

    private final Set<RESTServletContainer> servers = new HashSet<>();
    
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
    
    public Set<RESTServletContainer> getServers() {
    	return servers;
    }
    
    public void run(Collection<RESTServletContainer> containers) {
    	servers.addAll(containers);
    	
    	TDServlet td = new TDServlet();
    	CollectionServlet tdCollection = new CollectionServlet(td);
    	
    	RDFDocumentServlet vocab = new RDFDocumentServlet();
    	CollectionServlet vocabCollection = new CollectionServlet(vocab);
    	
    	TDLookUpEpServlet tdLookUpEp = new TDLookUpEpServlet();
    	TDLookUpResServlet tdLookUpRes = new TDLookUpResServlet();
    	TDLookUpSemServlet tdLookUpSem = new TDLookUpSemServlet(td);
      TDFramingServlet tdFraming = new TDFramingServlet(td);
    	
    	for (RESTServletContainer s : containers) {
    		s.addServletWithMapping("/td", tdCollection);
    		s.addServletWithMapping("/vocab", vocabCollection);
    		s.addServletWithMapping("/td-lookup/ep", tdLookUpEp);
    		s.addServletWithMapping("/td-lookup/res", tdLookUpRes);
    		s.addServletWithMapping("/td-lookup/sem", tdLookUpSem);
    		s.addServletWithMapping("/td-lookup/frame", tdFraming);
    		s.start();
    	}
    }
    
    public static void main(String[] args) throws Exception {
    	// Default values
        int portCoAP = DEFAULT_COAP_PORT;
        int portHTTP = DEFAULT_HTTP_PORT;
        String queryEndpoint = null; // SPARQL query endpoint
        String updateEndpoint = null; // SPARQL update endpoint
        String username = null;
        String password = null;
        
        // Handle input
        Options options = new Options();
        
        options.addOption("q", true, "SPARQL query endpoint URI for remote storage (main memory storage if not provided).");
        options.addOption("u", true, "SPARQL update endpoint URI for remote storage (same as query endpoint if not provided).");
        options.addOption("a", true, "SPARQL update endpoint auth credentials, user:pw (no auth if not provided).");
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
        if (cmd.hasOption("q")) {
        	queryEndpoint = cmd.getOptionValue("q");
        	
        	if (cmd.hasOption("u")) {
        		updateEndpoint = cmd.getOptionValue("u");
        	} else {
        		updateEndpoint = queryEndpoint;
        	}
        	
        	if (cmd.hasOption("a")) {
        		String credentials = cmd.getOptionValue("a");
        		if (credentials.contains(":")) {
        			String[] splitted = credentials.split(":");
        			username = splitted[0];
        			password = splitted[1];
        		}
        	}
        }
        if (cmd.hasOption("help")) {
        	// Automatically generate help statement
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("thingweb-directory", options);
        	System.exit(0);
        }

        // initiate SPARQL client
        if (queryEndpoint != null) {
        	if (username != null && password != null) {
        		Connector.init(queryEndpoint, updateEndpoint, username, password);
        	} else {
                Connector.init(queryEndpoint, updateEndpoint);
        	}
        } else {
            Connector.init();
        }
        
        // create and start REST server instances
        Set<RESTServletContainer> servers = new HashSet<>();
        servers.add(new CoAPServer(portCoAP));
        servers.add(new HTTPServer(portHTTP));
        
        ThingDirectory.get().run(servers);
    }
    
}