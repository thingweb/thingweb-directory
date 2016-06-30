package de.thingweb.repository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.jena.atlas.lib.StrUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDFS;

import de.thingweb.repository.rest.BadRequestException;
import de.thingweb.repository.rest.RESTException;
import de.thingweb.repository.rest.RESTHandler;
import de.thingweb.repository.rest.RESTResource;
import de.thingweb.repository.rest.RESTServerInstance;

public class ThingDescriptionCollectionHandler extends RESTHandler {
	
	public ThingDescriptionCollectionHandler(List<RESTServerInstance> instances) {
		super("td", instances);
	}
	
	@Override
	public RESTResource get(URI uri, Map<String, String> parameters) throws RESTException {

		/*
		if (!parameters.containsKey("query") || parameters.get("query").isEmpty()) {
			// TODO also check query's validity
	    	throw new BadRequestException();
	    }
	    */
	  
		RESTResource resource = new RESTResource(name(uri), this);
		resource.contentType = "application/ld+json";
		resource.content = "{";
		
		List<String> tds = new ArrayList<String>();
		String query;
		
		// Normal SPARQL query
		if (parameters.containsKey("query") && !parameters.get("query").isEmpty()) {
			
			query = parameters.get("query");
			try {
				tds = ThingDescriptionUtils.listThingDescriptions(query);
			} catch (Exception e) {
				throw new BadRequestException();
			}
			
		} else if (parameters.containsKey("query-text") && !parameters.get("query-text").isEmpty()) { // Full text search query
			
			query = parameters.get("query-text");
			try {
				tds = ThingDescriptionUtils.listThingDescriptionsFromTextSearch(query);
			} catch (Exception e) {
				throw new BadRequestException();
			}
			
		} else {
			// TODO also check query's validity
		    throw new BadRequestException();
		}
		
		// Retrieve Thing Description(s)
		for (int i = 0; i < tds.size(); i++) {
			URI td = URI.create(tds.get(i));
			
			try {
				ThingDescriptionHandler h = new ThingDescriptionHandler(td.toString(), instances);
				RESTResource res = h.get(td, new HashMap<String, String>());
				// TODO check TD's content type
				
		        resource.content += "\"" + td.getPath() + "\": " + res.content;
		        if (i < tds.size() - 1) {
		        	resource.content += ",";
		        }
				
			} catch (RESTException e) { // Life time is invalid and TD was removed
				// remove ","
				if (resource.content.endsWith(",")) {
					resource.content = resource.content.substring(0, resource.content.length() -1);
				}
				continue;
				
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("Unable to retrieve Thing Description " + td.getPath());
			}
		}
		
		resource.content += "}";
		return resource;
	}

	@Override
	public RESTResource post(URI uri, Map<String, String> parameters, InputStream payload) throws RESTException {
		// to add new thing description to the collection
		String id = generateID();
		URI resourceUri = URI.create(normalize(uri) + "/" + id);
		Dataset dataset = Repository.get().dataset;
		List<String> keyWords;

		dataset.begin(ReadWrite.WRITE);
		try {
		  String data = ThingDescriptionUtils.streamToString(payload);
		  
			Model tdb = dataset.getNamedModel(resourceUri.toString());
			tdb.read(new ByteArrayInputStream(data.getBytes()), "", "JSON-LD");
			// TODO check TD validity

      		tdb = dataset.getDefaultModel();
      		tdb.createResource(resourceUri.toString()).addProperty(DC.source, data);

      		// Get key words from statements
			ThingDescriptionUtils utils = new ThingDescriptionUtils();
			Model newThing = dataset.getNamedModel(resourceUri.toString());
			keyWords = utils.getModelKeyWords(newThing);

			// Store key words as triple: ?g_id rdfs:comment "keyWordOrWords"
			tdb.getResource(resourceUri.toString()).addProperty(RDFS.comment, StrUtils.strjoin(" ", keyWords));
      
			addToAll("/td/" + id, new ThingDescriptionHandler(id, instances));
			dataset.commit();
			// TODO remove useless return
			RESTResource resource = new RESTResource(resourceUri.toString(), new ThingDescriptionHandler(id, instances));
			return resource;
		} catch (IOException e) {
		  throw new BadRequestException();
		} catch (Exception e) {
			throw new RESTException();
		} finally {
			dataset.end();
		}
	}

	private String normalize(URI uri) {
		if (!uri.getScheme().equals("http")) {
			return uri.toString().replace(uri.getScheme(), "http");
		}
		return uri.toString();
	}
	
	private String name(URI uri) {
		String path = uri.getPath();
		if (path.contains("/")) {
			return path.substring(uri.getPath().lastIndexOf("/") + 1);
		}
		return path;
	}
	
	private String generateID() {
	  // TODO better way?
	  String id = UUID.randomUUID().toString();
	  return id.substring(0, id.indexOf('-'));
	}

}
