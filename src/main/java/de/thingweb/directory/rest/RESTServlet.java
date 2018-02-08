package de.thingweb.directory.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import de.thingweb.directory.ThingDirectory;

/**
 * 
 * Performs standard REST processing like content negotiation
 * and linking.
 *
 * @author Victor Charpenay
 * @creation 06.02.2018
 *
 */
public abstract class RESTServlet extends HttpServlet {
	
	protected static final String ACCEPT_HEADER = "Accept";
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String ct = req.getHeader(ACCEPT_HEADER);
		if (ct != null && !acceptsContentType(ct)) {
			resp.sendError(406); // Not Acceptable
		}
	}

	// TODO use Servlet Filters instead?
	protected abstract String[] getAcceptedContentTypes();
	
	private boolean acceptsContentType(String ct) {
		String regex = ct.replaceAll("[*]", ".*");
		for (String accepted : getAcceptedContentTypes()) {
			if (accepted.matches(regex)) {
				return true;
			}
		}
		return false;
	}
	
}
