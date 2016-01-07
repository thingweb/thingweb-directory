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
import org.apache.jena.rdf.model.Model;
import org.apache.jena.tdb.TDBFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.thingweb.desc.DescriptionParser;
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
  		Model td = null;
  			String query = new String();
  			query = "CONSTRUCT {?s ?p ?o} FROM <" + uri + "> WHERE {?s ?p ?o}";
  
  			try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
  
  				td = qexec.execConstruct();
  
  			}
  			
  			resource.contentType = "application/ld+json";
  			StringWriter wr = new StringWriter();
  			td.write(wr, "JSON-LD");
  			resource.content = DescriptionParser.reshape(wr.toString());
  			return resource;
    }
    catch (JsonProcessingException e)
    {
      throw new BadRequestException();
    }
    catch (IOException e)
    {
      throw new RESTException();
    } finally {
      dataset.end();
    }
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
