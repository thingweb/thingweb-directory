package de.thingweb.directory.resources;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;

import de.thingweb.directory.ThingDirectory;
import de.thingweb.directory.rest.BadRequestException;
import de.thingweb.directory.rest.CollectionResource;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTResource;
import de.thingweb.repository.vocabulary.TD;

public class VocabularyCollectionResource extends CollectionResource {

	public VocabularyCollectionResource(String path) {
		super(path, RDFDocument.factory());
		
		// TODO create child resources for all graphs already in the RDF store.
	}
	
	@Override
	public RESTResource post(Map<String, String> parameters, InputStream payload) throws RESTException {
		Model base = RDFDocument.read(parameters, payload);
		OntModel vocab = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, base);

		RESTResource root = null;
		
		if (parameters.containsKey(RESTResource.PARAMETER_CONTENT_TYPE)) {
			// forces default RDF format
			parameters.remove(RESTResource.PARAMETER_CONTENT_TYPE);
		}

		ExtendedIterator<Ontology> it = vocab.listOntologies();
		while (it.hasNext()) {
			Ontology o = it.next();
			
			String id = vocab.getNsURIPrefix(o.getURI());

			Model axioms = isRootOntology(o.getURI(), vocab) ? vocab : vocab.getImportedModel(o.getURI());

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			axioms.write(out, RDFDocument.DEFAULT_FORMAT);
			ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());

			RESTResource res = super.post(parameters, in);
			
			if (isRootOntology(o.getURI(), vocab)) {
				root = res;
			}

			ThingDirectory.LOG.info(String.format("Registered RDFS/OWL vocabulary %s (id: %s)", o.getURI(), id));
		}

		if (root == null) {
			throw new BadRequestException();
		} else {
			return root;
		}
	}

	@Override
	protected String generateChildID() {
		// TODO transmit prefix as resource name to parent
		return super.generateChildID();
	}
	
	private boolean isRootOntology(String uri, OntModel m) {
		return !m.contains(null, OWL.imports, ResourceFactory.createResource(uri));
	}

}
