package de.thingweb.directory.coap;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.network.Exchange;

import de.thingweb.directory.rest.CollectionItemServlet;
import de.thingweb.directory.rest.CollectionServlet;

public class CoAPServletWrapper extends CoapResource {

	private final HttpServlet servlet;
	
	public CoAPServletWrapper(String name, HttpServlet servlet) {
		super(name);
		this.servlet = servlet;
	}
	
	@Override
	public void handleRequest(Exchange exchange) {
		CoAPServletRequest req = new CoAPServletRequest(exchange.getRequest());
		CoAPServletResponse resp = new CoAPServletResponse();
		try {
			servlet.service(req, resp);
			
			if (resp.getStatus() == HttpServletResponse.SC_CREATED) {
				String loc = resp.getHeader(CollectionServlet.LOCATION_HEADER);
				add(new CoAPServletWrapper(loc, ((CollectionServlet) servlet).getItemServlet()));
			}
			
			exchange.sendResponse(resp.asResponse());
		} catch (ServletException | IOException e) {
			// TODO
		}
	}
	
}
