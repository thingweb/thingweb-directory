package de.thingweb.directory.coap;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.server.resources.Resource;

import de.thingweb.directory.rest.CollectionItemServlet;
import de.thingweb.directory.rest.CollectionServlet;
import de.thingweb.directory.rest.RESTServlet;
import de.thingweb.directory.rest.RESTServletContainer;

public class CoAPServer implements RESTServletContainer {

	protected CoapServer server;
	protected Thread t;

	private int port;

	public CoAPServer(int port) {
		this.port = port;
	}
	
	@Override
	public void addCollectionWithMapping(String path, CollectionServlet coll, CollectionItemServlet item) {
		addServletWithMapping(path, coll);
		addServletWithMapping(path + "/*", item);
	}
	
	@Override
	public void addServletWithMapping(String path, RESTServlet servlet) {
		Resource res = new CoAPServletWrapper(servlet);
		res.setPath(path); // FIXME process regex in path
		server.add(res);
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

}
