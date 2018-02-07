package de.thingweb.directory.servlet;

import de.thingweb.directory.rest.CollectionServlet;

public class TDCollectionServlet extends CollectionServlet {

	public TDCollectionServlet() {
		super(new TDServlet());
	}
	
}
