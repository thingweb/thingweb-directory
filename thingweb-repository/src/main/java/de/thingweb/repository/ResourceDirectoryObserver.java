package de.thingweb.repository;

import java.util.List;
import java.util.Map;
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
 *This class sets a CoAP client that will observe the specified 
 *ResourceDirectory URI in order to import Thing Descriptions of those things 
 *that register to it. When a thing is registered in the Resource Directory, 
 *an observation event occurs that sends the URI of that new thing to the 
 *observers. This class uses that URI to GET the thing description.
 *
 *In the same way, when a thing is no longer registered in the Resource
 *Directory, an observation event occurs and the thing description is 
 *deleted from the database. A deregistered thing is identified because its
 *URI would not be found in the ResourceDirectory response.
 *
 */
public class ResourceDirectoryObserver {
private String rd_uri;
private String td_uri;
private CoapObserveRelation relation;
private CoapClient client;
private HashMap<String, Boolean> things_map; // keeps track of the updates in the RD
private HashMap<String, Boolean> things_uris; // keeps track of the registered things

private final String url_re = "(coap?|http)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";

private List<RESTServerInstance> servers;

public ResourceDirectoryObserver(String uri, List<RESTServerInstance> srvs) {
	rd_uri = uri;
	td_uri = "http://www.example.com";

	servers = srvs;
	client = new CoapClient(rd_uri + "/rd");
	things_map = new HashMap<String, Boolean>();
	things_uris = new HashMap<String, Boolean>();
	
	// load thing_uris with the URIs stored in the repository's database
	for (String td_uri: ThingDescriptionUtils.listThingDescriptionsUri()) {
	//System.out.println(td_uri);
	this.things_uris.put(td_uri, true);
}

	// observing
	relation = client.observe(
		new CoapHandler() {
		@Override public void onLoad(CoapResponse response) {
			String content = response.getResponseText();
			System.out.println(content);

			// Extract the url's
			Matcher m = Pattern.compile(url_re).matcher(content);
			things_map.clear();

			while (m.find()) {
			
				String thing_url = m.group();
				things_map.put(thing_url, true);
				
				// Check if a new resource registers
				if (!things_uris.containsKey(thing_url)) {
					
					// Get TD
					CoapClient cli = new CoapClient(thing_url);
					cli.get(new CoapHandler() {
						@Override public void onLoad(CoapResponse response) {
			
							// Add TD
							ThingDescriptionCollectionHandler tdch = new ThingDescriptionCollectionHandler(servers);
							try {
								Map<String,String> parameters = new HashMap<String,String>();
								parameters.put("ep", thing_url);
								tdch.post(new URI(td_uri + "/td"), parameters, new ByteArrayInputStream(response.getPayload()));
							
							} catch (Exception e) {
								System.err.println(e.getMessage());
							}
						}
		
						@Override public void onError() {
							System.err.println("Failed");
						}
					});
					
					things_uris.put(thing_url, true);
			
				}
			
			}
			
			// Check if a known resource has deregistered, if so remove TD
			removeDeregisteredThingDescriptions();
			
		}

		@Override public void onError() {
			System.err.println("Failed");
		}
	});
	//relation.proactiveCancel();
}

 

/**
 * @brief Removes deregistered thing descriptions from the repository's database.
 * 
 * First, checks if a resource has deregistered from the Resource Directory.
 * If deregistered, then deletes it from the database.
 */
private void removeDeregisteredThingDescriptions() {
	
	/*
	System.out.println("\nRegistered tds: ");
	for (String uri : things_map.keySet()) {
		System.out.println(uri);
		System.out.println("");
	}
	
	System.out.println("\nStored things: ");
	for (String uri : things_uris.keySet()) {
		System.out.println(uri);
		System.out.println("");
	} 
	*/
	
	for (String uri : this.things_uris.keySet()) {
		
		if (!this.things_map.containsKey(uri)) {
		
			String source_id = "";
			try {
				source_id = ThingDescriptionUtils.getThingDescriptionIdFromUri(uri);
				
				System.out.println("Removing description with id " + source_id);
				
				// delete the thing description
				ThingDescriptionHandler tdh = new ThingDescriptionHandler(source_id, servers);
				try {
					tdh.delete(new URI(source_id), null, null);
					this.things_uris.remove(uri);
					System.out.println("Successfully removed description of URI " + uri);
					
				} catch (Exception e) {
					System.err.println(e.getMessage());
				}
			
			} catch (Exception e) {
				System.err.println(e.getMessage());
			}
			
		}
	}
	
}

}