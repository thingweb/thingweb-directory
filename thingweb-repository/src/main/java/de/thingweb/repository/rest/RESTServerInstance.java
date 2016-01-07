package de.thingweb.repository.rest;


public interface RESTServerInstance
{
  
  public void add(String path, RESTHandler handler);
  public void delete(String path);
  
  public void start();
  public void stop();
  public void join();
  
}
