package de.thingweb.directory.handlers;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.thingweb.directory.ThingDescriptionUtils;
import de.thingweb.directory.rest.BadRequestException;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTHandler;
import de.thingweb.directory.rest.RESTResource;
import de.thingweb.directory.rest.RESTServerInstance;

public class TDLookUpHandler extends RESTHandler {

	public TDLookUpHandler(List<RESTServerInstance> instances) {
		super("td-lookup", instances);
	}
	
}
