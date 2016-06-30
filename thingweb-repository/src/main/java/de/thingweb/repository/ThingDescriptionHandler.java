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

    // Check if life time is invalid
    if (!ThingDescriptionUtils.checkLifeTime(uri)) {
      try {
      delete(uri, null, null);
      } finally {
        throw new RESTException();
      }
    }

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

    try {
      String data = ThingDescriptionUtils.streamToString(payload);
  		Model td = ModelFactory.createDefaultModel();
  		// TODO find a way to know the base IRI in the document...
  		td.read(new ByteArrayInputStream(data.getBytes()), "", "JSON-LD");
  
  		if (td.isEmpty()) {
  			throw new BadRequestException();
  		}

      // Save properties of td's creation (kept on the default graph)
      ThingDescriptionUtils utils = new ThingDescriptionUtils();
      Model tdb = dataset.getDefaultModel();
      String created, modified, lifetime, lt, endpointName;
      created = tdb.getResource(uri.toString()).getProperty(DCTerms.created).getString();
      modified = utils.getCurrentDateTime(0);
      lifetime = tdb.getResource(uri.toString()).getProperty(DCTerms.dateAccepted).getString();
      endpointName = tdb.getResource(uri.toString()).getProperty(RDFS.isDefinedBy).getString();
      
      // Check if life time value is given
      if (parameters.containsKey(LIFE_TIME) && !parameters.get(LIFE_TIME).isEmpty()) {
        lt = parameters.get(LIFE_TIME);
        lifetime = new ThingDescriptionUtils().getCurrentDateTime(Integer.parseInt(lt));
        // TODO enforce a minimal lifetime
      }

      tdb.createResource(uri.toString()).removeProperties().addLiteral(DC.source, data);
      dataset.replaceNamedModel(uri.toString(), td);

      // Store properties. Update modified date and lifetime (if given)
      tdb.getResource(uri.toString()).addProperty(DCTerms.created, created);
      tdb.getResource(uri.toString()).addProperty(DCTerms.modified, modified);
      tdb.getResource(uri.toString()).addProperty(DCTerms.dateAccepted, lifetime);
      tdb.getResource(uri.toString()).addProperty(RDFS.isDefinedBy, endpointName);

      // Get key words of new content and store it
      Model newThing = dataset.getNamedModel(uri.toString());
      List<String> keyWords = utils.getModelKeyWords(newThing);
      tdb.getResource(uri.toString()).addProperty(RDFS.comment, StrUtils.strjoin(" ", keyWords));

			dataset.commit();
    } catch (IOException e) {
      throw new BadRequestException();
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
      dataset.getDefaultModel().createResource(uri.toString()).removeProperties();
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
