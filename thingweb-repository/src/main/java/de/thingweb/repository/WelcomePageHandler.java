package de.thingweb.repository;

import java.net.URI;
import java.util.List;
import java.util.Map;

import de.thingweb.repository.rest.RESTException;
import de.thingweb.repository.rest.RESTHandler;
import de.thingweb.repository.rest.RESTResource;
import de.thingweb.repository.rest.RESTServerInstance;

public class WelcomePageHandler extends RESTHandler
{
  
  private static final String MESSAGE = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>"
      + "Thingweb-Repository: a repository for W3C Thing Descriptions"
      + "</title></head><body>"
      + "Hi. This is a headless API.<br>"
      + "See our documentation <a href=\"https://github.com/thingweb/thingweb-repository#interacting-with-a-thingweb-repository-server\">on Github</a>."
      + "</body></html>";

  public WelcomePageHandler(List<RESTServerInstance> instances)
  {
    super("", instances);
  }
  
  @Override
  public RESTResource get(URI uri, Map<String, String> parameters) throws RESTException
  {
    RESTResource welcomePage = new RESTResource("", this);
    welcomePage.content = MESSAGE;
    welcomePage.contentType = "text/html";
    return welcomePage;
  }
  
}
