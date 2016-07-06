package de.thingweb.repository.coap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.server.resources.Resource;

import de.thingweb.repository.WelcomePageHandler;
import de.thingweb.repository.rest.RESTHandler;
import de.thingweb.repository.rest.RESTServerInstance;

public class CoAPServer implements RESTServerInstance {

  protected CoapServer server;
  protected Thread t;
  
  public CoAPServer(int port, RESTHandler root)
  {
    server = new CoapServer(port) {
      @Override
      protected Resource createRoot()
      {
        return new CoAPRESTResource(root);
      }
    };
  }
  
  public void init(Map<String, RESTHandler> resources)
  {
    Map<String, String> tree = new HashMap<>(); // map of parent relations
    Map<String, Resource> map = new HashMap<>();
    
    // create the resource tree based on path
    for (String path : resources.keySet()) {
      map.put(path, new CoAPRESTResource(resources.get(path)));
      String lowest = null;
      String highest = null;
      for (String p : tree.keySet()) {
        if (path.contains(p) && (lowest == null || p.contains(lowest))) {
          lowest = p;
        }
        if (p.contains(path) && (highest == null || highest.contains(p))) {
          highest = p;
        }
      }
      tree.put(path, lowest);
      if (highest != null) {
        tree.put(highest, path);
      }
    }
    
    // add resources to their parent in the resource tree
    for (String path : resources.keySet()) {
      if (tree.get(path) == null) {
        server.add(map.get(path));
      } else {
        map.get(tree.get(path)).add(map.get(path));
      }
    }
  }
  
  @Override
  public void add(String path, RESTHandler handler) {
    // TODO currently assumes resources are added after their parents
    addRec(path, new CoAPRESTResource(handler), server.getRoot());
  }
  
  @Override
  public void delete(String path) {
    deleteRec(path, server.getRoot());
  }
  
  @Override
  public void start() {
    t = new Thread(new Runnable()
    {
      @Override
      public void run()
      {
        server.start();
      }
    });
    t.start();
  }
  
  @Override
  public void stop() {
    t.stop();
  }
  
  @Override
  public void join() {
    try
    {
      t.join();
    }
    catch (InterruptedException e)
    {
      // TODO
      e.printStackTrace();
    }
  }
  
  protected void addRec(String path, Resource resource, Resource parent) {
    for (Resource r : parent.getChildren()) {
      if (path.contains(path(r))) {
        addRec(path, resource, r);
        return;
      }
    }
    parent.add(resource);
  }
  
  protected void deleteRec(String path, Resource parent) {
    if (path(parent).equals(path)) {
      parent.getParent().remove(parent);
      return;
    }
    for (Resource r : parent.getChildren()) {
      if (path.contains(path(r))) {
        deleteRec(path, r);
        return;
      }
    }
  }
  
  protected String path(Resource r) {
    return r.getPath() + r.getName();
  }
  
}
