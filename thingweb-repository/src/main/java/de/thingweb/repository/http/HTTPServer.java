package de.thingweb.repository.http;

import java.util.EnumSet;
import java.util.Map;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.eclipse.jetty.servlets.CrossOriginFilter;

import de.thingweb.repository.rest.RESTHandler;
import de.thingweb.repository.rest.RESTServerInstance;

public class HTTPServer implements RESTServerInstance {

  protected Server server;
  protected ServletHandler handler;
  
  public HTTPServer(int port, RESTHandler root)
  {
    server = new Server(port);
    handler = new ServletHandler();
    server.setHandler(handler);
    add("", root);
    
    configureCORS();
  }
  
  @Override
  public void add(String path, RESTHandler restHandler) {
    ServletHolder holder = new ServletHolder(new HTTPRESTResource(restHandler));
    handler.addServletWithMapping(holder, path);
  }
  
  @Override
  public void delete(String path)
  {
    ServletMapping mapping = handler.getServletMapping(path);
    ServletMapping[] mappings = new ServletMapping [handler.getServletMappings().length - 1];
    ServletHolder[] servlets = new ServletHolder [handler.getServlets().length - 1];
    int mLength = 0, sLength = 0;
    
    for (ServletMapping m : handler.getServletMappings()) {
      if (!m.equals(mapping)) {
        mappings[mLength++] = m;
      }
    }
    
    for (ServletHolder s : handler.getServlets()) {
      if (!s.equals(handler.getServlet(mapping.getServletName()))) {
        servlets[sLength++] = s;
      }
    }
    
    handler.setServletMappings(mappings);
    handler.setServlets(servlets);
  }
  
  @Override
  public void start()
  {
    try
    {
      server.start();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
  
  @Override
  public void stop()
  {
    try
    {
      server.stop();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
  
  @Override
  public void join()
  {
    try
    {
      server.join();
    }
    catch (InterruptedException e)
    {
      e.printStackTrace();
    }
  }
  
  protected void configureCORS() {
	  
	  FilterHolder holder = new FilterHolder(new CrossOriginFilter());
	  holder.setInitParameter("allowedOrigins", "*"); // TODO - restrict this
	  holder.setInitParameter("allowedMethods", "GET,POST,PUT,DELETE,HEAD,OPTIONS");
	  holder.setInitParameter("allowedCredentials", "true");
	  
	  this.handler.addFilterWithMapping(holder, "/*", EnumSet.of(DispatcherType.REQUEST));
  }
  
}
