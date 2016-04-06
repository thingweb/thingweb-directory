package de.thingweb.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.vocabulary.DC;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.thingweb.desc.ThingDescriptionParser;
import de.thingweb.repository.rest.BadRequestException;
import de.thingweb.repository.rest.RESTException;
import de.thingweb.repository.rest.RESTHandler;
import de.thingweb.repository.rest.RESTResource;
import de.thingweb.repository.rest.RESTServerInstance;

public class ThingDescriptionHandler extends RESTHandler {
	
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
		Dataset dataset = Repository.get().dataset;
		dataset.begin(ReadWrite.WRITE);
		Model td = dataset.getDefaultModel();
		// TODO find a way to know the base IRI in the document...
		td.read(payload, "", "JSON-LD");

		if (td.isEmpty()) {
			throw new BadRequestException();
		}
		try {
			dataset.replaceNamedModel(uri.toString(), td);
			dataset.commit();
		} catch (Exception e) {
			// TODO distinguish between client and server errors
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
      dataset.removeNamedModel(uri.toString());
      deleteToAll(uri.getPath());
      dataset.commit();
    } catch (Exception e) {
      // TODO distinguish between client and server errors
      throw new RESTException();
    } finally {
      dataset.end();
    }
	}

}
