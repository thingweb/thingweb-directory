package de.thingweb.directory.servlet;


public class TDCollectionServlet extends CollectionServlet {

	public TDCollectionServlet() {
		super(new TDServlet());
	}
	
}
