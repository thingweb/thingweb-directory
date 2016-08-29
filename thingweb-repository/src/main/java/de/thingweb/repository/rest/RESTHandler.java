package de.thingweb.repository.rest;

import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.util.List;
import java.util.Map;

public abstract class RESTHandler {
	
	private final String name;
	protected List<RESTServerInstance> instances;
	
	public RESTHandler(String name, List<RESTServerInstance> instances) {
		this.name = name;
		this.instances = instances;
	}
	
	public String name() {
		return name;
	}

	public RESTResource get(URI uri, Map<String, String> parameters) throws RESTException {
		throw new MethodNotAllowedException();
	}
	
	public RESTResource post(URI uri, Map<String, String> parameters, InputStream payload) throws RESTException {
		throw new MethodNotAllowedException();
	}
	
	public void put(URI uri, Map<String, String> parameters, InputStream payload) throws RESTException {
		throw new MethodNotAllowedException();
	}
	
	public void delete(URI uri, Map<String, String> parameters, InputStream payload) throws RESTException {
		throw new MethodNotAllowedException();
	}
	
	public RESTResource observe(URI uri, Map<String, String> parameters) throws RESTException {
		throw new MethodNotAllowedException();
	}
	
	protected void addToAll(String path, RESTHandler handler) {
	  new AddWorker(path, handler).start();
	}
	
	protected void deleteToAll(String path) {
	  new DeleteWorker(path).start();
	}
	
	protected class AddWorker extends Thread {
	  
	  private String path;
	  private RESTHandler handler;
	  
	  public AddWorker(String path, RESTHandler handler)
    {
	    this.path = path;
	    this.handler = handler;
    }
	  
	  @Override
	  public void run()
	  {
      for (RESTServerInstance i : instances) {
        i.add(path, handler);
      }
	  }
	  
	}
  
  protected class DeleteWorker extends Thread {
    
    private String path;
    
    public DeleteWorker(String path)
    {
      this.path = path;
    }
    
    @Override
    public void run()
    {
      for (RESTServerInstance i : instances) {
        i.delete(path);
      }
    }
    
  }
	
}
