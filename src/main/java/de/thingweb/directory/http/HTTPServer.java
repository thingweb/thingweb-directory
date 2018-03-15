package de.thingweb.directory.http;

import java.net.URL;
import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.util.resource.PathResource;
import org.eclipse.jetty.util.resource.Resource;

import de.thingweb.directory.rest.CollectionItemServlet;
import de.thingweb.directory.rest.CollectionServlet;
import de.thingweb.directory.rest.RESTServlet;
import de.thingweb.directory.rest.RESTServletContainer;

public class HTTPServer extends RESTServletContainer {

	protected Server server;
	protected ServletHandler handler;

	public HTTPServer(int port) {
		server = new Server(port);
		handler = new ServletHandler();
		
		ResourceHandler publicHandler = new ResourceHandler();
		publicHandler.setBaseResource(Resource.newClassPathResource("public"));
		publicHandler.setDirectoriesListed(true);
		publicHandler.setWelcomeFiles(new String[] { "index.html" });
		
		HandlerList handlers = new HandlerList();
		handlers.setHandlers(new Handler[] {
			publicHandler, // serves files in '/public'
			handler, // uses mapped servlets
			new DefaultHandler() // returns 404
		});

		configureCORS();
		
		server.setHandler(handlers);
	}
	
	@Override
	public void addServletWithMapping(String path, RESTServlet servlet) {
		ServletHolder holder = new ServletHolder(servlet);
		handler.addServletWithMapping(holder, path);
		
		super.addServletWithMapping(path, servlet);
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

	private void configureCORS() {
		FilterHolder holder = new FilterHolder(new CrossOriginFilter());
		holder.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*"); // TODO - restrict this
		holder.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,POST,PUT,DELETE,HEAD,OPTIONS");
		holder.setInitParameter(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM, "true");

		handler.addFilterWithMapping(holder, "/*", EnumSet.of(DispatcherType.REQUEST));
	}

}
