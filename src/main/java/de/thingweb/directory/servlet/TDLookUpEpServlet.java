package de.thingweb.directory.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.thingweb.directory.rest.RESTServlet;

public class TDLookUpEpServlet extends RESTServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		super.doGet(req, resp);

		// TODO
		resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
	}
	
	@Override
	protected String[] getAcceptedContentTypes() {
		// TODO
		return new String [0];
	}

}
