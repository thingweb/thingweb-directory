package de.thingweb.repository;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.vocabulary.DC;

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
	  if (!parameters.containsKey("query") || parameters.get("query").isEmpty()) {
	    // TODO also check query's validity
	    throw new BadRequestException();
	  }
	  
		RESTResource resource = new RESTResource(name(uri), this);
		resource.contentType = "application/ld+json";
		resource.content = "{";
		
		List<String> tds = new ArrayList<String>();
    try
    {
      tds = ThingDescriptionUtils.listThingDescriptions(parameters.get("query"));
    } catch (Exception e) {
      throw new BadRequestException();
    }
    
    for (int i = 0; i < tds.size(); i++) {
      URI td = URI.create(tds.get(i));
      try
      {
        RESTResource res = new ThingDescriptionHandler(td.toString(), instances).get(td, new HashMap<String, String>());
        // TODO check TD's content type
        resource.content += "\"" + td.getPath() + "\": " + res.content;
        if (i < tds.size() - 1) {
          resource.content += ",";
        }
      }
      catch (Exception e)
      {
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
		dataset.begin(ReadWrite.WRITE);
		
		try {
		  String data = ThingDescriptionUtils.streamToString(payload);
		  
			Model tdb = dataset.getNamedModel(resourceUri.toString());
			tdb.read(new ByteArrayInputStream(data.getBytes()), "", "JSON-LD");
			// TODO check TD validity

      		tdb = dataset.getDefaultModel();
      		tdb.createResource(resourceUri.toString()).addProperty(DC.source, data);
      
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
