package de.thingweb.directory.http;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.thingweb.directory.ThingDirectory;
import de.thingweb.directory.rest.BadRequestException;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTResource;

public class HTTPResourceContainer extends HttpServlet {

  private static final long serialVersionUID = 8480825672944956465L;
  
  private RESTResource resource;
  
  public HTTPResourceContainer(RESTResource resource) {
    super();
    this.resource = resource;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
  {
    try {
      resource.get(params(req), resp.getOutputStream());
    resp.setContentType(resource.getContentType());
    } catch (BadRequestException e) {
      resp.sendError(400);
    } catch (RESTException e) {
      resp.sendError(500);
    }
  }
  
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
  {
    try {
      RESTResource child = resource.post(params(req), req.getInputStream());
      resp.setStatus(201);
      resp.setHeader("Location", child.getPath());
    } catch (BadRequestException e) {
      resp.sendError(400);
    } catch (RESTException e) {
      resp.sendError(500);
    }
  }
  
  @Override
  protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
  {
    try {
        resource.put(params(req), req.getInputStream());
    } catch (BadRequestException e) {
      resp.sendError(400);
    } catch (RESTException e) {
      resp.sendError(500);
    }
  }
  
  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
  {
    try {
        resource.delete(params(req));
    } catch (BadRequestException e) {
      resp.sendError(400);
    } catch (RESTException e) {
      resp.sendError(500);
    }
  }
  
  private Map<String, String> params(HttpServletRequest req) {
	  Map<String, String> p = new HashMap<>();
	  
	  // content negotiation headers
	  String accept = req.getHeader("Accept");
	  if (accept != null && !accept.isEmpty()) {
		  p.put(RESTResource.PARAMETER_ACCEPT, req.getHeader("Accept"));
	  }
	  String ct = req.getHeader("Content-Type");
	  if (ct != null && !ct.isEmpty()) {
		  p.put(RESTResource.PARAMETER_CONTENT_TYPE, req.getHeader("Content-Type"));
	  }

	  // query parameters
	  for (Entry<String, String[]> e : req.getParameterMap().entrySet()) {
		  String val = "";
		  for (String v : e.getValue()) {
			  val += v + ",";
		  }
		  val = val.substring(0, val.length() - 1);
		  p.put(e.getKey(), val);
	  }
	  
	  return p;
  }
  
  private URI uri(String path) {
    return URI.create(ThingDirectory.get().getBaseURI() + path);
  }
  
  
  
}
