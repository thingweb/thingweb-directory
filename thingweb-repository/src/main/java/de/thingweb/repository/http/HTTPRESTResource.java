package de.thingweb.repository.http;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.thingweb.repository.Repository;
import de.thingweb.repository.rest.BadRequestException;
import de.thingweb.repository.rest.RESTException;
import de.thingweb.repository.rest.RESTHandler;
import de.thingweb.repository.rest.RESTResource;

public class HTTPRESTResource extends HttpServlet {

  private static final long serialVersionUID = 8480825672944956465L;
  
  protected RESTHandler handler;
  
  public HTTPRESTResource(RESTHandler handler) {
    super();
    this.handler = handler;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
  {
    try {
      RESTResource res = handler.get(uri(req.getRequestURI()), concat(req.getParameterMap()));
      resp.setContentType(res.contentType);
      resp.getWriter().write(res.content);
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
      RESTResource res = handler.post(uri(req.getRequestURI()), concat(req.getParameterMap()), req.getInputStream());
      resp.setStatus(201);
      resp.setHeader("Location", res.path);
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
      handler.put(uri(req.getRequestURI()), concat(req.getParameterMap()), req.getInputStream());
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
      handler.delete(uri(req.getRequestURI()), concat(req.getParameterMap()), req.getInputStream());
    } catch (BadRequestException e) {
      resp.sendError(400);
    } catch (RESTException e) {
      resp.sendError(500);
    }
  }
  
  protected Map<String, String> concat(Map<String, String[]> params) {
    Map<String, String> p = new HashMap<>();
    for (Entry<String, String[]> e : params.entrySet()) {
      String val = "";
      for (String v : e.getValue()) {
        val += v + ",";
      }
      val = val.substring(0, val.length() - 1);
      p.put(e.getKey(), val);
    }
    return p;
  }
  
  protected URI uri(String path) {
    return URI.create(Repository.get().baseURI + path);
  }
  
  
  
}
