package de.thingweb.repository;

import java.util.List;
import java.lang.String;
import java.lang.Boolean;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.HashMap;

import java.net.URI;
import java.io.ByteArrayInputStream;

import de.thingweb.repository.rest.RESTServerInstance;
import de.thingweb.repository.ThingDescriptionCollectionHandler;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
//import org.eclipse.californium.coap.MediaTypeRegistry;

/** @brief Observe the ResourceDirectory Server to import Thing Descriptions.
 *
 *  This class sets a CoAP client that will observe the specified 
 *  ResourceDirectory URI in order to import Thing Descriptions of those things 
 *  that register to it. When a thing is registered in the Resourse Directory, 
 *  an observation event occures that sends the URI of that new thing to the 
 *  observers. This class uses that URI to GET the thing description.
 *
 */
public class ResourceDirectoryObserver {
  private String rd_uri;
  private CoapObserveRelation relation;
  private CoapClient client;
  private HashMap<String, Boolean> things_map;

  private final String url_re = "(coap?|http)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";

  private List<RESTServerInstance> servers;

  public ResourceDirectoryObserver(String uri, List<RESTServerInstance> srvs) {
    rd_uri = uri;

    servers = srvs;
    client = new CoapClient(rd_uri + "/rd");
    things_map = new HashMap<String, Boolean>();

    // observing
    relation = client.observe(
        new CoapHandler() {
          @Override public void onLoad(CoapResponse response) {
            String content = response.getResponseText();
            System.out.println(content);

            // Extract the url's
            Matcher m = Pattern.compile(url_re).matcher(content);

            while (m.find()) {
              if (!things_map.containsKey(m.group())) {
                String thing_url = m.group();

                things_map.put(thing_url, true);

                // Get TD
                System.out.println(thing_url);
                CoapClient cli = new CoapClient(thing_url);

                cli.get(new CoapHandler() {
                  @Override public void onLoad(CoapResponse response) {
                    String content = response.getResponseText();
                    System.out.println(content);
		
                    // add the thing description
                    ThingDescriptionCollectionHandler tdch = new ThingDescriptionCollectionHandler(servers);
                    try {
                      tdch.post(new URI("coap://localhost:5683/td"), null, new ByteArrayInputStream(response.getPayload()));
                    } catch (Exception e) {
                      System.err.println(e.getMessage());
                    }
                  }

                  @Override public void onError() {
                    System.err.println("Failed");
                  }
                });

              }
            }
          }

          @Override public void onError() {
            System.err.println("Failed");
          }
    });
    //relation.proactiveCancel();
  }
}
