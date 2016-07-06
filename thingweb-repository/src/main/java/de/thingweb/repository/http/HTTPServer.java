package de.thingweb.repository.http;

import java.util.Map;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;

import de.thingweb.repository.ThingDescriptionCollectionHandler;
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
  
}
