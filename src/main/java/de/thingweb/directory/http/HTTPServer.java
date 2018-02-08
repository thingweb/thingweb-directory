package de.thingweb.directory.http;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServlet;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.eclipse.jetty.servlets.CrossOriginFilter;

import de.thingweb.directory.rest.CollectionItemServlet;
import de.thingweb.directory.rest.CollectionResource;
import de.thingweb.directory.rest.CollectionServlet;
import de.thingweb.directory.rest.IndexResource;
import de.thingweb.directory.rest.RESTResource;
import de.thingweb.directory.rest.RESTServerInstance;
import de.thingweb.directory.rest.RESTServlet;
import de.thingweb.directory.rest.RESTServletContainer;

public class HTTPServer implements RESTServletContainer {

	protected IndexResource root;
	protected Server server;
	protected ServletHandler handler;

	public HTTPServer(int port) {
		server = new Server(port);
		handler = new ServletHandler();
		server.setHandler(handler);

		configureCORS();
	}
	
	@Override
	public void addCollectionWithMapping(String path, CollectionServlet coll, CollectionItemServlet item) {
		addServletWithMapping(path, coll);
		addServletWithMapping(path + "/*", item);
	}
	
	@Override
	public void addServletWithMapping(String path, RESTServlet servlet) {
		ServletHolder holder = new ServletHolder(servlet);
		handler.addServletWithMapping(holder, path);
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
		holder.setInitParameter("allowedOrigins", "*"); // TODO - restrict this
		holder.setInitParameter("allowedMethods", "GET,POST,PUT,DELETE,HEAD,OPTIONS");
		holder.setInitParameter("allowedCredentials", "true");

		handler.addFilterWithMapping(holder, "/*", EnumSet.of(DispatcherType.REQUEST));
	}

}
