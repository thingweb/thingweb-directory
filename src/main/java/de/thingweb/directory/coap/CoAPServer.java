package de.thingweb.directory.coap;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.server.resources.Resource;

import de.thingweb.directory.rest.CollectionItemServlet;
import de.thingweb.directory.rest.CollectionServlet;
import de.thingweb.directory.rest.RESTServlet;
import de.thingweb.directory.rest.RESTServletContainer;

public class CoAPServer extends RESTServletContainer {

	protected CoapServer server;
	protected Thread t;

	public CoAPServer(int port) {
		server = new CoapServer(port);
	}
	
	@Override
	public void addServletWithMapping(String path, RESTServlet servlet) {
		if (path.startsWith("/") && path.endsWith("/*")) {
			// path-prefix mappings
			// managed by collection wrapper
		} else if (path.startsWith("*.")) {
			// TODO extension mapping
		} else if (path.equals("/")) {
			// TODO default mapping
		} else {
			// exact mapping
			String name = path.substring(path.lastIndexOf('/') + 1);
			Resource res = new CoAPServletWrapper(name, servlet);
			addRec(path, res, server.getRoot());
		}
		
		super.addServletWithMapping(path, servlet);
	}

	@Override
	public void start() {
		// TODO server can only be started after setIndex() is called
		t = new Thread(new Runnable() {
			@Override
			public void run() {
				server.start();
			}
		});
		t.start();
	}

	@Override
	public void stop() {
		t.stop();
	}

	@Override
	public void join() {
		try {
			t.join();
		} catch (InterruptedException e) {
			// TODO
			e.printStackTrace();
		}
	}
	  
	private void addRec(String path, Resource resource, Resource parent) {
		int end = path.indexOf('/', 1);
		String name = path.substring(1, end < 0 ? path.length() : end);

		if (resource.getName().equals(name)) {
			parent.add(resource);
		} else {
			if (parent.getChild(name) == null) {
				parent.add(new CoapResource(name));
			}
			String subpath = path.substring(path.indexOf('/', 1));
			addRec(subpath, resource, parent.getChild(name));
		}
	}

}
