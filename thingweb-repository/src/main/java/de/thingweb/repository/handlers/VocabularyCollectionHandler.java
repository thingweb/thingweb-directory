package de.thingweb.repository.handlers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.jena.atlas.json.JsonParseException;
import org.apache.jena.atlas.lib.StrUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDFS;

import de.thingweb.repository.Repository;
import de.thingweb.repository.ThingDescription;
import de.thingweb.repository.ThingDescriptionUtils;
import de.thingweb.repository.VocabularyUtils;
import de.thingweb.repository.rest.BadRequestException;
import de.thingweb.repository.rest.NotFoundException;
import de.thingweb.repository.rest.RESTException;
import de.thingweb.repository.rest.RESTHandler;
import de.thingweb.repository.rest.RESTResource;
import de.thingweb.repository.rest.RESTServerInstance;

public class VocabularyCollectionHandler extends RESTHandler {
	
	public VocabularyCollectionHandler(List<RESTServerInstance> instances) {
		super("vocab", instances);
	}
	
	@Override
	public RESTResource get(URI uri, Map<String, String> parameters) throws RESTException {
	  
		RESTResource resource = new RESTResource(name(uri), this);
		resource.contentType = "application/json";
		resource.content = "[";
		
		Set<String> vocabs = new HashSet<String>();
		
		// Return all TDs
		try {
			vocabs = VocabularyUtils.listVocabularies();
		} catch (Exception e) {
			throw new BadRequestException();
		}
		
		Iterator<String> it = vocabs.iterator();
		while (it.hasNext()) {
			String vocab = it.next();
			URI vocabUri = URI.create(vocab);
			resource.content += "\"" + vocabUri.getPath() + "\"";
			if (it.hasNext()) {
				resource.content += ",";
			}
		}
		
		resource.content += "]";
		return resource;
	}

	@Override
	public RESTResource post(URI uri, Map<String, String> parameters, InputStream payload) throws RESTException {
		
		String data = "";
		String ontologyUri = null;
		try {
			data = ThingDescriptionUtils.streamToString(payload);
			ontologyUri = new URI(data).toString();
			data = null;
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new BadRequestException();
		} catch (URISyntaxException e2) {
			// do nothing
		}
		
		// TODO Check if new vocab already registered in the dataset

		// to add new thing description to the collection
		String id = generateID();
		URI resourceUri = URI.create(normalize(uri) + "/" + id);
		Dataset dataset = Repository.get().dataset;

		dataset.begin(ReadWrite.WRITE);
		try {
			
			OntModel ontology = ModelFactory.createOntologyModel();
			if (data == null) {
				ontology.read(ontologyUri.toString(), "Turtle");
			} else {
				ontologyUri = "http://example.org/"; // TODO
				ontology.read(new ByteArrayInputStream(data.getBytes("UTF-8")), ontologyUri, "Turtle");
			}
			
			ExtendedIterator<Ontology> it = ontology.listOntologies();
			if (!it.hasNext()) {
					throw new BadRequestException();
			}
			while (it.hasNext()) {
				// TODO manage imports
				Ontology o = it.next();
				String prefix = ontology.getNsURIPrefix(o.getURI());
				if (prefix != null && !prefix.isEmpty()) { // either no prefix found or base URI
					id = prefix;
				}
				System.out.println(o.getURI() + " -> " + id);
			}

			dataset.addNamedModel(resourceUri.toString(), ontology);

			Model tdb = dataset.getDefaultModel();
			
			Date currentDate = new Date(System.currentTimeMillis());
			DateFormat f = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			tdb.getResource(resourceUri.toString()).addProperty(DCTerms.source, ontologyUri);
			tdb.getResource(resourceUri.toString()).addProperty(DCTerms.created, f.format(currentDate));
	  
			addToAll("/vocab/" + id, new VocabularyHandler(id, instances));
			dataset.commit();

			Repository.LOG.info(String.format("Registered RDFS/OWL vocabulary %s (%d triples)", id, ontology.size()));
			
			// TODO remove useless return
			RESTResource resource = new RESTResource("/vocab/" + id, new VocabularyHandler(id, instances));
			return resource;

		} catch (Exception e) {
			e.printStackTrace();
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