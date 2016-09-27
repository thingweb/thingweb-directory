package de.thingweb.repository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.apache.jena.atlas.lib.StrUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDFS;

import de.thingweb.repository.rest.BadRequestException;
import de.thingweb.repository.rest.NotFoundException;
import de.thingweb.repository.rest.RESTException;
import de.thingweb.repository.rest.RESTHandler;
import de.thingweb.repository.rest.RESTResource;
import de.thingweb.repository.rest.RESTServerInstance;

public class ThingDescriptionHandler extends RESTHandler {

	// for Resource Directory
	public static final String LIFE_TIME = "lt";
	
	public ThingDescriptionHandler(String id, List<RESTServerInstance> instances) {
		super(id, instances);
	}
	
	@Override
	public RESTResource get(URI uri, Map<String, String> parameters) throws RESTException {
		RESTResource resource = new RESTResource(uri.toString(),this);

		Dataset dataset = Repository.get().dataset;
		dataset.begin(ReadWrite.READ);
		
		try {
			String q = "SELECT ?str WHERE { <" + uri + "> <" + DC.source + "> ?str }";
			QueryExecution qexec = QueryExecutionFactory.create(q, dataset);
			ResultSet result = qexec.execSelect();
			
			if (result.hasNext()) {
				resource.contentType = "application/ld+json";
				resource.content = result.next().get("str").asLiteral().getLexicalForm();
			} else {
				throw new RESTException();
			}
		} finally {
			dataset.end();
		}
		
		return resource;
	}
	
	@Override
	public void put(URI uri, Map<String, String> parameters, InputStream payload) throws RESTException {
		
		String data = "";
		try {
			data = ThingDescriptionUtils.streamToString(payload);
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new BadRequestException();
		}
		
		// Check if new TD has uris already registered in the dataset
		if (ThingDescriptionUtils.hasInvalidURI(data, uri.toString())) {
			throw new BadRequestException();
		}
		
		Dataset dataset = Repository.get().dataset;
		dataset.begin(ReadWrite.WRITE);
		
		try {
			Model td = ModelFactory.createDefaultModel();
			Model tdb = dataset.getDefaultModel();
			String created, modified, lifetime, lt, endpointName;
			
			// Check if payload is empty but parameters not
			if (payload.available() <= 0 && parameters.containsKey(LIFE_TIME) && !parameters.get(LIFE_TIME).isEmpty()) {
				
				tdb.getResource(uri.toString()).removeAll(DCTerms.dateAccepted);
					
				// Update lifetime
				lt = parameters.get(LIFE_TIME);
				lifetime = new ThingDescriptionUtils().getCurrentDateTime(Integer.parseInt(lt));
				tdb.getResource(uri.toString()).addProperty(DCTerms.dateAccepted, lifetime);
				
			} else { // Payload is not empty
				
				// Save properties of td's creation (kept on the default graph)
				ThingDescriptionUtils utils = new ThingDescriptionUtils();
				created = tdb.getResource(uri.toString()).getProperty(DCTerms.created).getString();
				modified = utils.getCurrentDateTime(0);
				lt = "86400";
				endpointName = tdb.getResource(uri.toString()).getProperty(RDFS.isDefinedBy).getString();
				
				// TODO find a way to know the base IRI in the document...
				td.read(new ByteArrayInputStream(data.getBytes()), endpointName, "JSON-LD");
		  
				if (td.isEmpty()) {
					throw new BadRequestException();
				}
				
				// Check if life time value is given, otherwise use default
				if (parameters.containsKey(LIFE_TIME) && !parameters.get(LIFE_TIME).isEmpty()) {
					lt = parameters.get(LIFE_TIME);
					// TODO enforce a minimal lifetime
				}
				lifetime = new ThingDescriptionUtils().getCurrentDateTime(Integer.parseInt(lt));
				
				// Remove properties and add new content
				tdb.getResource(uri.toString()).removeProperties().addLiteral(DC.source, data);
				
				// Store properties. Update modified date and lifetime (if given)
				tdb.getResource(uri.toString()).addProperty(DCTerms.created, created);
				tdb.getResource(uri.toString()).addProperty(DCTerms.modified, modified);
				tdb.getResource(uri.toString()).addProperty(DCTerms.dateAccepted, lifetime);
				tdb.getResource(uri.toString()).addProperty(RDFS.isDefinedBy, endpointName);
				
				// Get key words of new content and store it
				Model newThing = dataset.getNamedModel(uri.toString());
				List<String> keyWords = utils.getModelKeyWords(newThing);
				tdb.getResource(uri.toString()).addProperty(RDFS.comment, StrUtils.strjoin(" ", keyWords));
				
				dataset.replaceNamedModel(uri.toString(), td);
			}
			
			// Update priority queue
			ThingDescription t = new ThingDescription(uri.toString(), lifetime);
			Repository.get().tdQueue.remove(t);
			Repository.get().tdQueue.add(t);
			Repository.get().setTimer();
			
			dataset.commit();
			
		} catch (IOException e) {
			e.printStackTrace();
			throw new BadRequestException();
		} catch (Exception e) {
			// TODO distinguish between client and server errors
			e.printStackTrace();
			throw new RESTException();
		} finally {
			dataset.end();
		}
	}
	
	@Override
	public void delete(URI uri, Map<String, String> parameters, InputStream payload) throws RESTException {
		Dataset dataset = Repository.get().dataset;
		dataset.begin(ReadWrite.WRITE);
		try {
			dataset.getDefaultModel().createResource(uri.toString()).removeProperties();
			dataset.removeNamedModel(uri.toString());
			deleteToAll(uri.getPath());
			dataset.commit();
			
			// Remove from priority queue
			ThingDescription td = new ThingDescription(uri.toString());
			Repository.get().tdQueue.remove(td);
			Repository.get().setTimer();
						
		} catch (Exception e) {
			// TODO distinguish between client and server errors
			throw new RESTException();
		} finally {
			dataset.end();
		}
	}

}