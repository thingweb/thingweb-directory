package de.thingweb.directory.http;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.thingweb.directory.rest.CollectionResource;
import de.thingweb.directory.rest.NotFoundException;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTResource;

public class HTTPCollectionContainer extends HTTPResourceContainer {
	
	private static final long serialVersionUID = 848125434795956465L;
	  
	public HTTPCollectionContainer(CollectionResource resource) {
		super(resource);
	}
	
	@Override
	protected RESTResource select(HttpServletRequest req) throws NotFoundException {
		String path = req.getRequestURI();
		String name = path.substring(path.lastIndexOf("/") + 1);
		
		CollectionResource coll = (CollectionResource) resource;
		for (RESTResource child : coll.getChildren()) {
			if (name.equals(child.getName())) {
				return child;
			}
		}
		
		throw new NotFoundException();
	}

}
