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
package org.eclipse.thingweb.directory.app;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;

public class Main {
	
	public static final Integer DEFAULT_HTTP_PORT = 8080;

	public static void main(String[] args) throws Exception {		
		Options options = new Options();
		options.addOption("p", true, "HTTP port number. Default is 8080.");
		options.addOption("h", false, "This help message.");

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);

		if (cmd.hasOption("h")) {
		    HelpFormatter formatter = new HelpFormatter();
		    formatter.printHelp("thingweb-directory", options);
			System.exit(0);
		}
		
		// TODO add SPARQL endpoint options

		int port = DEFAULT_HTTP_PORT;
		if (cmd.hasOption("p")) {
			port = Integer.parseInt(cmd.getOptionValue("p"));
		}
		
		Server server = initServer(port);
		
		server.start();
		// TODO use logger instead
		server.dumpStdErr();
		// TODO signal to stop server
		server.join();
	}
	
	private static Server initServer(Integer port) throws Exception {
		try {
			Resource base = Resource.newClassPathResource("webapp");
			Resource desc = base.addPath("WEB-INF/web.xml");
			
			WebAppContext ctx = new WebAppContext();
			ctx.setContextPath("/");
			ctx.setBaseResource(base);
			ctx.setDescriptor(desc.toString());

			Server server = new Server(port);
			server.setHandler(ctx);
			
			return server;
		} catch (Exception e) {
			throw new RuntimeException("Cannot find or open web.xml descriptor file.", e);
		}
	}
	
}
