package de.thingweb.directory.coap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.CoapExchange;

import de.thingweb.directory.resources.TDCollectionResource;
import de.thingweb.directory.rest.BadRequestException;
import de.thingweb.directory.rest.NotFoundException;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTResource;

public class CoAPResourceContainer extends CoapResource {

	private RESTResource resource;
	
	public CoAPResourceContainer(RESTResource resource) {
		super(resource.getName());
		this.resource = resource;
//
		if (resource instanceof TDCollectionResource) {
			this.getAttributes().addResourceType("core.rd");
			super.setObservable(true);
		}
//		else if (resource instanceof TDLookUpHandler) {
//		this.getAttributes().addResourceType("core.rd-lookup");
//	}
	}
	
	public void hasChanged() {
		super.changed();
	}
	
	@Override
	public void handleGET(CoapExchange exchange) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			resource.get(params(exchange), out);
			exchange.respond(ResponseCode.VALID, out.toByteArray(), toContentFormatCode(resource.getContentType()));
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
			RESTResource child = resource.post(params(exchange), payload(exchange));
			Response response = new Response(ResponseCode.CREATED);
			response.setOptions(new OptionSet().addLocationPath(child.getPath()));
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
			resource.put(params(exchange), payload(exchange));
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
			resource.delete(params(exchange));
			exchange.respond(ResponseCode.DELETED);
		} catch (BadRequestException e) {
			exchange.respond(ResponseCode.BAD_REQUEST);
		} catch (RESTException e) {
			exchange.respond(ResponseCode.INTERNAL_SERVER_ERROR);
		}
		this.hasChanged();
	}
	
	private Map<String, String> params(CoapExchange exchange) {
		Map<String, String> params = new HashMap<>();
		try {
			// content negotiation headers
			Integer acceptCode = exchange.getRequestOptions().getAccept();
			if (acceptCode > 0) {
				params.put(RESTResource.PARAMETER_ACCEPT, MediaTypeRegistry.toString(acceptCode));
			}
			Integer cfCode = exchange.getRequestOptions().getContentFormat();
			if (cfCode > 0) {
				params.put(RESTResource.PARAMETER_CONTENT_TYPE, MediaTypeRegistry.toString(cfCode));
			}
			
			// query parameters
			for (String pair : exchange.getRequestOptions().getUriQuery()) {
				pair = URLDecoder.decode(pair, "UTF-8");
				if (pair.contains("=")) {
					String[] p = pair.split("=");
					if (p.length > 1) {
						params.put(p[0], p[1]);
					} else {
						params.put(p[0], "");
					}
				}
			}
		} catch (UnsupportedEncodingException e) {
			System.err.println("UTF-8 encoding not supported!");
		}
		return params;
	}
	
	private InputStream payload(CoapExchange exchange) {
	  return new ByteArrayInputStream(exchange.getRequestPayload());
	}

	private int toContentFormatCode(String contentType) {
		if (contentType.equals("application/ld+json")) {
			contentType = "application/json";
		}
    	return MediaTypeRegistry.parse(contentType);
	}

}
