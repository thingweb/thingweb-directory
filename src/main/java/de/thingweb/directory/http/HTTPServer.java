package de.thingweb.directory.http;

import java.util.EnumSet;
import java.util.Map;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServlet;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.eclipse.jetty.servlets.CrossOriginFilter;

import de.thingweb.directory.ThingDirectory;
import de.thingweb.directory.rest.CollectionResource;
import de.thingweb.directory.rest.IndexResource;
import de.thingweb.directory.rest.RESTResource;
import de.thingweb.directory.rest.RESTServerInstance;

public class HTTPServer implements RESTServerInstance {

	protected IndexResource root;
  protected Server server;
  protected ServletHandler handler;
  
  public HTTPServer(int port)
  {
    server = new Server(port);
    handler = new ServletHandler();
    server.setHandler(handler);
    
    configureCORS();
  }
  
	@Override
	public void onCreate(RESTResource resource) {
		if (!isMapped(resource)) {
			ServletHolder holder = new ServletHolder(new HTTPResourceContainer(resource));
		    handler.addServletWithMapping(holder, resource.getPath());
		    
		    resource.addListener(this);
		    
		    if (resource instanceof CollectionResource && !(resource instanceof IndexResource)) {
		    	// TODO deal with other resources than index that have fixed sub-resources
				HttpServlet collectionContainer = new HTTPCollectionContainer((CollectionResource) resource);
			    ServletHolder collectionHolder = new ServletHolder(collectionContainer);
			    handler.addServletWithMapping(collectionHolder, resource.getPath() + "/*");
		    }
		}
	}
  
  @Override
	public void onDelete(RESTResource resource) {
	    ServletMapping mapping = handler.getServletMapping(resource.getPath());
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
	public void setIndex(IndexResource index) {
		root = index;
		onCreate(index);
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
	  
	  handler.addFilterWithMapping(holder, "/*", EnumSet.of(DispatcherType.REQUEST));
  }
  
  protected boolean isMapped(RESTResource res) {
	  ServletMapping[] mappings = handler.getServletMappings();
	  
	  if (mappings != null) {
		  for (ServletMapping m : mappings) {
			  for (String spec : m.getPathSpecs()) {
				  String substitute = spec.replace("*", res.getName());
				  if (substitute.equals(res.getPath())) {
					  return true;
				  }
			  }
		  }
	  }
	  
	  return false;
  }
  
  protected CollectionResource getParent(CollectionResource root, RESTResource res) {
	  if (root.getChildren().contains(res)) {
		  return root;
	  }
	  
	  for (RESTResource child : root.getChildren()) {
		  if (child instanceof CollectionResource) {
			  CollectionResource p = getParent((CollectionResource) child, res);
			  if (p != null) {
				  return p;
			  }
		  }
	  }
	  
	  return null;
  }
  
}
