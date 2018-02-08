package de.thingweb.directory.coap;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.network.Exchange;

public class CoAPServletWrapper extends CoapResource {

	private final HttpServlet servlet;
	
	public CoAPServletWrapper(HttpServlet servlet) {
		super(servlet.getServletName());
		this.servlet = servlet;
	}
	
	@Override
	public void handleRequest(Exchange exchange) {
		CoAPServletRequest req = new CoAPServletRequest(exchange.getRequest());
		CoAPServletResponse resp = new CoAPServletResponse();
		try {
			servlet.service(req, resp);
			exchange.sendResponse(resp.asResponse());
		} catch (ServletException | IOException e) {
			// TODO
		}
	}
	
}
