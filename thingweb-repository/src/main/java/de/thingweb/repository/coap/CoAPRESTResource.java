package de.thingweb.repository.coap;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.CoapExchange;

import de.thingweb.repository.Repository;
import de.thingweb.repository.ThingDescriptionCollectionHandler;
import de.thingweb.repository.handlers.TDLookUpHandler;
import de.thingweb.repository.rest.BadRequestException;
import de.thingweb.repository.rest.NotFoundException;
import de.thingweb.repository.rest.RESTException;
import de.thingweb.repository.rest.RESTHandler;
import de.thingweb.repository.rest.RESTResource;

public class CoAPRESTResource extends CoapResource {

	protected RESTHandler handler;
	
	public CoAPRESTResource(RESTHandler handler) {
		super(handler.name());
		this.handler = handler;

		if (handler instanceof ThingDescriptionCollectionHandler) {
			this.getAttributes().addResourceType("core.rd");
			super.setObservable(true);
			
		} else if (handler instanceof TDLookUpHandler) {
			this.getAttributes().addResourceType("core.rd-lookup");
		}
	}
	
	public void hasChanged() {
		super.changed();
	}
	
	@Override
	public void handleGET(CoapExchange exchange) {
		try {
			RESTResource res = handler.get(uri(), params(exchange));
			exchange.respond(ResponseCode.VALID, res.content, toContentFormatCode(res.contentType));
		} catch (BadRequestException e) {
			exchange.respond(ResponseCode.BAD_REQUEST);
		} catch (NotFoundException e) {
			exchange.respond(ResponseCode.NOT_FOUND);
		} catch (RESTException e) {
			exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR);
		}
	}
	
	public void handlePOST(CoapExchange exchange) {
		try {
			RESTResource resource = handler.post(uri(), params(exchange), payload(exchange));
			Response response = new Response(ResponseCode.CREATED);
			response.setOptions(new OptionSet().addLocationPath(trim(resource.path)));
			exchange.respond(response);
		} catch (BadRequestException e) {
			exchange.respond(ResponseCode.BAD_REQUEST);
		} catch (RESTException e) {
			exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR);
		}
		this.hasChanged();
	}
	
	@Override
	public void handlePUT(CoapExchange exchange) {
		try {
			handler.put(uri(), params(exchange), payload(exchange));
			exchange.respond(ResponseCode.CHANGED);
		} catch (BadRequestException e) {
			exchange.respond(ResponseCode.BAD_REQUEST);
		} catch (RESTException e) {
			exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR);
		}
		this.hasChanged();
	}

	@Override
	public void handleDELETE(CoapExchange exchange) {
		try {
			handler.delete(uri(), params(exchange), payload(exchange));
			exchange.respond(ResponseCode.DELETED);
		} catch (BadRequestException e) {
			exchange.respond(ResponseCode.BAD_REQUEST);
		} catch (RESTException e) {
			exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR);
		}
		this.hasChanged();
	}
	
	protected URI uri() {
		return URI.create(Repository.get().baseURI + getURI());
	}
	
	protected Map<String, String> params(CoapExchange exchange) {
		Map<String, String> params = new HashMap<>();
	  try
	{
		for (String pair : exchange.getRequestOptions().getUriQuery()) {
		pair = URLDecoder.decode(pair, "UTF-8");
		if (pair.contains("=")) {
		  String[] p = pair.split("=");
		  if (p.length > 1)
		  {
			  params.put(p[0], p[1]);  
		  }
		  else
		  {
			  params.put(p[0], "");
		  }
		}
		}
	}
	catch (UnsupportedEncodingException e)
	{
	  System.err.println("UTF-8 encoding not supported!");
	}
		return params;
	}
	
	protected InputStream payload(CoapExchange exchange) {
	  return new ByteArrayInputStream(exchange.getRequestPayload());
	}
	
	protected String trim(String path) {
	  if (path.charAt(0) == '/') {
		return path.substring(1);
	  }
	  return path;
	}
	
	protected int toContentFormatCode(String contentType) {
	  switch (contentType) {
      // TODO 50 -> application/json, not ld+json
	    case "application/ld+json": return 50;
	    default: return 0;
	  }
	}

}
