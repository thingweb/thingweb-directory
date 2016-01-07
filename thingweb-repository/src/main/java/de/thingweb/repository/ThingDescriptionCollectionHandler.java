package de.thingweb.repository;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URI;
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
	  if (!parameters.containsKey("query")) {
	    // TODO also check query's validity
	    throw new BadRequestException();
	  }
	  
		RESTResource resource = new RESTResource(name(uri), this);
		resource.contentType = "application/ld+json";
		resource.content = "{";
		
		List<String> tds = ThingDescriptionUtils.listThingDescriptions(parameters.get("query"));
    for (int i = 0; i < tds.size(); i++) {
      String td = tds.get(i);
      try
      {
        RESTResource res = new ThingDescriptionHandler(td, instances).get(URI.create(td), new HashMap<String, String>());
        // TODO check TD's content type
        resource.content += "\"" + td + "\": " + res.content;
        if (i < tds.size() - 1) {
          resource.content += ",";
        }
      }
      catch (Exception e)
      {
        e.printStackTrace();
        System.err.println("Unable to retrieve Thing Description " + td);
      }
    }

    resource.content += "}";
		return resource;
	}

	@Override
	public RESTResource post(URI uri, Map<String, String> parameters, InputStream payload) throws RESTException {
		// to add new thing description to the collection
		
			UUID uuid = UUID.randomUUID();
			URI resourceUri = URI.create(normalize(uri) + "/" + uuid);
			Dataset dataset = Repository.get().dataset;
			dataset.begin(ReadWrite.WRITE);
			try {
				Model tdb = dataset.getNamedModel(resourceUri.toString());
				tdb.read(payload, "", "JSON-LD");
				// TODO check TD validity
				tdb.commit();
				tdb.close();
				addToAll("/td/" + uuid, new ThingDescriptionHandler(uuid.toString(), instances));
				dataset.commit();
				// TODO remove useless return
				RESTResource resource = new RESTResource("/td/" + uuid, new ThingDescriptionHandler(uuid.toString(), instances));
				return resource;
			} catch (Exception e) {
				throw new BadRequestException();
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

}
