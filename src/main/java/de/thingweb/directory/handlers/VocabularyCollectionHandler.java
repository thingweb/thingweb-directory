package de.thingweb.directory.handlers;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
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
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

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
		
		Dataset dataset = ThingDirectory.get().dataset;
		dataset.begin(ReadWrite.WRITE);
		try {
			String rootId = null;
			
			OntModel ontology = ModelFactory.createOntologyModel();
			if (data == null) {
				ontology.read(ontologyUri.toString(), "Turtle");
			} else {
				ontologyUri = "http://example.org/"; // TODO
				ontology.read(new ByteArrayInputStream(data.getBytes("UTF-8")), ontologyUri, "Turtle");
			}

			Model tdb = dataset.getDefaultModel();
			
			ExtendedIterator<Ontology> it = ontology.listOntologies();
			if (!it.hasNext()) {
					throw new BadRequestException();
			}
			while (it.hasNext()) {
				Ontology o = it.next();
				
				String prefix = ontology.getNsURIPrefix(o.getURI());
				// if no prefix found, generates id
				String id = (prefix != null && !prefix.isEmpty()) ? prefix : generateID();
				URI resourceUri = URI.create(normalize(uri) + "/" + id);
				
				OntModel axioms;
				if (isRootOntology(o.getURI(), ontology)) {
					rootId = id;
					axioms = ontology;
				} else {
					axioms = ontology.getImportedModel(o.getURI());
				}
				
				// TODO Check if the vocab isn't already registered in the dataset
				dataset.addNamedModel(resourceUri.toString(), axioms);
				
				Date currentDate = new Date(System.currentTimeMillis());
				DateFormat f = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
				tdb.getResource(resourceUri.toString()).addProperty(DCTerms.source, ontologyUri);
				tdb.getResource(resourceUri.toString()).addProperty(DCTerms.created, f.format(currentDate));

				addToAll("/vocab/" + id, new VocabularyHandler(id, instances));

				ThingDirectory.LOG.info(String.format("Registered RDFS/OWL vocabulary %s (id: %s)", o.getURI(), id));
			}
			
			dataset.commit();
			
			RESTResource resource = new RESTResource("/vocab/" + rootId, new VocabularyHandler(rootId, instances));
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
	
	private boolean isRootOntology(String uri, OntModel m) {
		return !m.contains(null, OWL.imports, ResourceFactory.createResource(uri));
	}

}