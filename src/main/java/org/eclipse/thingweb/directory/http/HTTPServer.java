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
package org.eclipse.thingweb.directory.http;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.thingweb.directory.rest.RESTServlet;
import org.eclipse.thingweb.directory.rest.RESTServletContainer;

public class HTTPServer extends RESTServletContainer {

	protected Server server;
	protected ServletContextHandler ctx;

	public HTTPServer(int port) {
		server = new Server(port);
		
		ctx = new ServletContextHandler();
		ctx.setContextPath("/");
		ctx.setWelcomeFiles(new String[] { "index.html" });
		
		ServletHolder h = new ServletHolder("default", DefaultServlet.class);
		h.setInitParameter("resourceBase", getPublicFolderBase());
		h.setInitParameter("dirAllowed", "true");
		ctx.addServlet(h, "/");

		FilterHolder holder = new FilterHolder(new CrossOriginFilter());
		holder.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*"); // TODO - restrict this
		holder.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,POST,PUT,DELETE,HEAD,OPTIONS");
		holder.setInitParameter(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM, "true");
		ctx.addFilter(holder, "/*", EnumSet.of(DispatcherType.REQUEST));
		
		HandlerList handlers = new HandlerList();
		handlers.setHandlers(new Handler[] {
			ctx, // uses mapped servlets & serves files in '/public'
			new DefaultHandler() // returns 404
		});
		
		server.setHandler(handlers);
	}
	
	@Override
	public void addServletWithMapping(String path, RESTServlet servlet) {
		ServletHolder holder = new ServletHolder(servlet);
		ctx.addServlet(holder, path);
		
		super.addServletWithMapping(path, servlet);
	}
	
	@Override
	public void addFilterWithMapping(String path, Filter filter) {
		FilterHolder holder = new FilterHolder(filter);
		ctx.addFilter(holder, path, EnumSet.of(DispatcherType.REQUEST));
	}

	@Override
	public void start() {
		try {
			server.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stop() {
		try {
			server.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void join() {
		try {
			server.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private String getPublicFolderBase() {
		Resource folder = Resource.newClassPathResource("public");
		try {
			return folder.toString();
		} finally {
			folder.close();
		}
	}

}
