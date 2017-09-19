package de.thingweb.directory.handlers;

import java.net.URI;
import java.util.List;
import java.util.Map;

import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTHandler;
import de.thingweb.directory.rest.RESTResource;
import de.thingweb.directory.rest.RESTServerInstance;

public class WelcomePageHandler extends RESTHandler
{
  
  private static final String MESSAGE = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>"
      + "Thingweb-Repository: a repository for W3C Thing Descriptions"
      + "</title></head><body>"
      + "Hi. This is a headless API.<br>"
      + "See the <a href=\"api.json\">OAS specification</a> for this API "
      + "or a textual documentation <a href=\"https://github.com/thingweb/thingweb-repository#interacting-with-a-thingweb-repository-server\">on Github</a>."
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
