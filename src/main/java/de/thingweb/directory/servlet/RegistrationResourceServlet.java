package de.thingweb.directory.servlet;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.thingweb.directory.ThingDirectory;

/**
 * 
 * Implements registration resource operations from the
 * CoRE Resource Directory specification (Sect. 5.4).
 *
 * @author Victor Charpenay
 * @creation 06.02.2018
 *
 */
public abstract class RegistrationResourceServlet extends CollectionItemServlet {

	/**
	 * Default lifetime: 24h (86,400s)
	 */
	public final static Integer DEFAULT_LIFETIME = 86400;
	
	public static final String PARAMETER_LIFETIME = "lt";
	
	public static final String PARAMETER_ENDPOINT = "ep";
	
	private static final String[] ACCEPTED = { "application/json" };
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String id = getItemID(req);
		if (hasExpired(id)) {
			resp.sendError(404); // Not Found
		}
		
		// TODO implement CoRE Link format representation
	}
	
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		int lt = DEFAULT_LIFETIME;
		if (req.getParameter(PARAMETER_LIFETIME) != null) {
			lt = Integer.parseInt(req.getParameter(PARAMETER_LIFETIME));
		}
		
		updateTimeout(getItemID(req), lt);
	}
	
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO ?
		super.doDelete(req, resp);
	}
	
	@Override
	protected String[] getAcceptedContentTypes() {
		return ACCEPTED;
	}
	
	protected String getBaseURI(HttpServletRequest req) {
		// TODO take ep into account
		return ThingDirectory.getBaseURI() + "/";
	}
	
	protected abstract boolean hasExpired(String id);
	
	protected abstract void updateTimeout(String id, int lifetime);

}
