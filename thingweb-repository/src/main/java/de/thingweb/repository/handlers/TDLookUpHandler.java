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

public class TDLookUpHandler extends RESTHandler {

	public TDLookUpHandler(List<RESTServerInstance> instances) {
		super("td-lookup", instances);
	}
	
}
