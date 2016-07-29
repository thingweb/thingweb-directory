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

import org.apache.jena.atlas.json.JsonParseException;
import org.apache.jena.atlas.lib.StrUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDFS;

import de.thingweb.repository.rest.BadRequestException;
import de.thingweb.repository.rest.NotFoundException;
import de.thingweb.repository.rest.RESTException;
import de.thingweb.repository.rest.RESTHandler;
import de.thingweb.repository.rest.RESTResource;
import de.thingweb.repository.rest.RESTServerInstance;

public class ThingDescriptionCollectionHandler extends RESTHandler {

	// for Resource Directory
	public static final String LIFE_TIME = "lt";
	public static final String END_POINT = "ep";
	
	public ThingDescriptionCollectionHandler(List<RESTServerInstance> instances) {
		super("td", instances);
	}
	
	@Override
	public RESTResource get(URI uri, Map<String, String> parameters) throws RESTException {
	  
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
			
		} else if (parameters.containsKey("text") && !parameters.get("text").isEmpty()) { // Full text search query
			
			query = parameters.get("text");
			try {
				tds = ThingDescriptionUtils.listThingDescriptionsFromTextSearch(query);
			} catch (Exception e) {
				throw new BadRequestException();
			}
			
		} else if (parameters.containsKey("rdf") && !parameters.get("rdf").isEmpty()) { // RDF type/value type query
			
			query = parameters.get("rdf");
			try {
				tds = ThingDescriptionUtils.listRDFTypeValues(query);
			} catch (Exception e) {
				throw new BadRequestException();
			}
			
			// Retrieve type values
			for (int i = 0; i < tds.size(); i++) {
				resource.content += "\"unit\": " + tds.get(i);
				if (i < tds.size() - 1) {
					resource.content += ",\n";
				}
			}
			
			resource.content += "}";
			return resource;
			
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
				
			}  catch (NotFoundException e) {
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
		
		resource.content += "}";
		return resource;
	}

	@Override
	public RESTResource post(URI uri, Map<String, String> parameters, InputStream payload) throws RESTException {

		// to register a resource following the standard
		String endpointName = "http://example.org/"; // this is temporary
		String lifeTime = "86400"; // 24 hours by default in seconds

		// TODO make it mandatory. The rest are optional
		if (parameters.containsKey(END_POINT) && !parameters.get(END_POINT).isEmpty()) {
			endpointName = parameters.get(END_POINT);	
		}
		
		if (parameters.containsKey(LIFE_TIME) && !parameters.get(LIFE_TIME).isEmpty()) {
			lifeTime = parameters.get(LIFE_TIME);
			// TODO enforce a minimal lifetime
		}

		// to add new thing description to the collection
		String id = generateID();
		URI resourceUri = URI.create(normalize(uri) + "/" + id);
		Dataset dataset = Repository.get().dataset;
		List<String> keyWords;

		dataset.begin(ReadWrite.WRITE);
		try {
			String data = ThingDescriptionUtils.streamToString(payload);
		  
			Model tdb = dataset.getNamedModel(resourceUri.toString());
			tdb.read(new ByteArrayInputStream(data.getBytes()), endpointName, "JSON-LD");
			// TODO check TD validity

			tdb = dataset.getDefaultModel();
			tdb.createResource(resourceUri.toString()).addProperty(DC.source, data);

			// Get key words from statements
			ThingDescriptionUtils utils = new ThingDescriptionUtils();
			Model newThing = dataset.getNamedModel(resourceUri.toString());
			keyWords = utils.getModelKeyWords(newThing);

			// Store key words as triple: ?g_id rdfs:comment "keyWordOrWords"
			tdb.getResource(resourceUri.toString()).addProperty(RDFS.comment, StrUtils.strjoin(" ", keyWords));

			// Store END_POINT and LIFE_TIME as triples
			String currentDate = utils.getCurrentDateTime(0);
			String lifetimeDate = utils.getCurrentDateTime(Integer.parseInt(lifeTime));
			tdb.getResource(resourceUri.toString()).addProperty(RDFS.isDefinedBy, endpointName);
			tdb.getResource(resourceUri.toString()).addProperty(DCTerms.created, currentDate);
			tdb.getResource(resourceUri.toString()).addProperty(DCTerms.modified, currentDate);
			tdb.getResource(resourceUri.toString()).addProperty(DCTerms.dateAccepted, lifetimeDate);
	  
			addToAll("/td/" + id, new ThingDescriptionHandler(id, instances));
			dataset.commit();
			// TODO remove useless return
			RESTResource resource = new RESTResource("/td/" + id, new ThingDescriptionHandler(id, instances));
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
