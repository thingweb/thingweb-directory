package de.thingweb.directory.handlers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RIOT;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

import de.thingweb.directory.ThingDirectory;
import de.thingweb.directory.ThingDescription;
import de.thingweb.directory.ThingDescriptionUtils;
import de.thingweb.directory.VocabularyUtils;
import de.thingweb.directory.rest.BadRequestException;
import de.thingweb.directory.rest.NotFoundException;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTHandler;
import de.thingweb.directory.rest.RESTResource;
import de.thingweb.directory.rest.RESTServerInstance;
import de.thingweb.repository.rdf.TD;
import de.thingweb.thing.MediaType;

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
		resource.contentType = "application/json";
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
				e.printStackTrace();
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
			// Return all TDs
			try {
				tds = ThingDescriptionUtils.listThingDescriptions("?s ?p ?o");
			} catch (Exception e) {
				throw new BadRequestException();
			}
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
		
		String data = "";
		try {
			data = ThingDescriptionUtils.streamToString(payload);
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new BadRequestException();
		}
		
		// to register a resource following the standard
		String endpointName = "http://example.org/"; // TODO
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
		Dataset dataset = ThingDirectory.get().dataset;
		Model schema = VocabularyUtils.mergeVocabularies();

		try {
			String format = "JSON-LD";
			if (parameters.containsKey(RESTHandler.PARAMETER_CONTENT_TYPE)) {
				String mediaType = parameters.get(RESTHandler.PARAMETER_CONTENT_TYPE);
				Lang l = RDFLanguages.contentTypeToLang(mediaType);
				if (l != null) {
					format = l.getName();
				} else {
					// TODO guess RDF specific type from generic media type (CoAP)
					ThingDirectory.LOG.debug("No RDF format for media type: " + mediaType + ". Assuming JSON-LD.");
				}
			}
			
			Model graph = ModelFactory.createDefaultModel();
			graph.read(new ByteArrayInputStream(data.getBytes()), endpointName, format);
			
			if (!format.equals("JSON-LD")) {
				// data source should be encoded in JSON-LD
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				graph.write(out, "JSON-LD");
				data = out.toString("UTF-8");
			}
			
			InfModel inf = ModelFactory.createInfModel(ReasonerRegistry.getOWLMicroReasoner(), schema, graph);
			// TODO check TD validity
			
			List<RESTResource> resources = new ArrayList<>();
			ResIterator it = inf.listResourcesWithProperty(RDF.type, TD.Thing);
			if (!it.hasNext()) {
					throw new BadRequestException();
			}
			while (it.hasNext()) {
				Resource root = it.next();
				
				String id = ThingDescriptionUtils.getThingDescriptionId(root);
				
				if (id != null) {
					ThingDirectory.LOG.info("TD already registered: " + root);
				} else {
					id = generateID();
					URI resourceUri = URI.create(normalize(uri) + "/" + id);
					List<String> keyWords;

					dataset.begin(ReadWrite.WRITE);
					Model tdModel = extractTD(root);
					dataset.addNamedModel(resourceUri.toString(), tdModel);

					Model tdb = dataset.getDefaultModel();
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

					ThingDirectory.LOG.info(String.format("Registered TD: %s (%d triples)", id, graph.size()));
					
					// Add to priority queue
					ThingDescription td = new ThingDescription(resourceUri.toString(), lifetimeDate);
					ThingDirectory.get().tdQueue.add(td);
					ThingDirectory.get().setTimer();
					
					dataset.end();
				}
				
				RESTResource resource = new RESTResource("/td/" + id, new ThingDescriptionHandler(id, instances));
				resources.add(resource);
			}
			
			// TODO Location header must be a single URI. Other TDs?
			return resources.get(0);

		} catch (Exception e) {
			e.printStackTrace();
			throw new RESTException();
		} finally {
			if (dataset.isInTransaction()) {
				dataset.end();
			}
		}
	}
	
	
	@Override
	public RESTResource observe(URI uri, Map<String, String> parameters) throws RESTException {
		
		return get(uri, null);
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
	
	private static Model extractTD(Resource root) {
		Model td = ModelFactory.createDefaultModel();
		  
		StmtIterator it = root.listProperties();
		while (it.hasNext()) {
			Statement st = it.next();
			td.add(st);
			if (!st.getPredicate().equals(RDF.type) && st.getObject().isResource()) {
				Resource node = st.getObject().asResource();
				if (!node.hasProperty(RDF.type, TD.Thing)) {
					// FIXME cycle detection (if interaction patterns reference each other)
					td.add(extractTD(node));
				}
			}
		}
		  
		return td;
	}

}