package de.thingweb.repository.handlers;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.thingweb.repository.ThingDescriptionHandler;
import de.thingweb.repository.ThingDescriptionUtils;
import de.thingweb.repository.rest.BadRequestException;
import de.thingweb.repository.rest.RESTException;
import de.thingweb.repository.rest.RESTHandler;
import de.thingweb.repository.rest.RESTResource;
import de.thingweb.repository.rest.RESTServerInstance;

public class TDLookUpEPHandler extends RESTHandler {

	public TDLookUpEPHandler(List<RESTServerInstance> instances) {
		super("ep", instances);
	}
	
	@Override
	public RESTResource get(URI uri, Map<String, String> parameters) throws RESTException {

		RESTResource resource = new RESTResource(name(uri), this);
		resource.contentType = "application/ld+json";
		resource.content = "{";
		
		// get all the registered endpoints
		List<String> eps = new ArrayList<String>();
		
		try {
			eps = ThingDescriptionUtils.listEndpoints();
		} catch (Exception e) {
			e.printStackTrace();
			throw new BadRequestException();
		}
		
		// retrieve the endpoints
		for (int i = 0; i < eps.size(); i++) {
			resource.content += "ep=\""+ eps.get(i) + "\""; 
			if (i < eps.size() - 1) {
	        	resource.content += ",";
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
