package de.thingweb.repository.handlers;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.thingweb.repository.ThingDescriptionHandler;
import de.thingweb.repository.ThingDescriptionUtils;
import de.thingweb.repository.rest.BadRequestException;
import de.thingweb.repository.rest.NotFoundException;
import de.thingweb.repository.rest.RESTException;
import de.thingweb.repository.rest.RESTHandler;
import de.thingweb.repository.rest.RESTResource;
import de.thingweb.repository.rest.RESTServerInstance;

public class TDLookUpEPHandler extends RESTHandler {

	// for Resource Directory
	public static final String END_POINT = "ep";

	public TDLookUpEPHandler(List<RESTServerInstance> instances) {
		super("ep", instances);
	}
	
	@Override
	public RESTResource get(URI uri, Map<String, String> parameters) throws RESTException {

		RESTResource resource = new RESTResource(name(uri), this);
		resource.contentType = "application/ld+json";
		resource.content = "{";
		
		List<String> tds = new ArrayList<String>();
		String ep = "";
		
		// Normal SPARQL query
		if (parameters.containsKey(END_POINT) && !parameters.get(END_POINT).isEmpty()) { 
			ep = parameters.get(END_POINT);
		}
		
		// Get the TD's
		try {
			tds = ThingDescriptionUtils.listThingDescriptions("?s ?p ?o .");
		} catch (Exception e) {
			throw new BadRequestException();
		}
		
		// Retrieve Thing Description(s)
		for (int i = 0; i < tds.size(); i++) {
			URI td = URI.create(tds.get(i));
			
			// Filter by endpoint if given
			if ( ep.equalsIgnoreCase(td.getPath()) || ep.isEmpty()) {
			
				try {
					ThingDescriptionHandler h = new ThingDescriptionHandler(td.toString(), instances);
					RESTResource res = h.get(td, new HashMap<String, String>());
					// TODO check TD's content type
							
					resource.content += "\"" + td.getPath() + "\": " + res.content;
					if (i < tds.size() - 1) {
						resource.content += ",";
					}
							
				} catch (NotFoundException e) {
					// remove ","
					if (resource.content.endsWith(",")) {
						resource.content = resource.content.substring(0, resource.content.length() -1);
					}
					continue; // Life time is invalid and TD was removed
					
				} catch (Exception e) {
					e.printStackTrace();
					System.err.println("Unable to retrieve Thing Description " + td.getPath());
				}
			}
		}
				
		resource.content += "}";	
		return resource;
	}
	
	private String name(URI uri) {
		
		String path = uri.getPath();
		if (path.contains("/")) {
			return path.substring(uri.getPath().lastIndexOf("/") + 1);
		}
		return path;
	}
}
