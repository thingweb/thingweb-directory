package de.thingweb.directory.resources;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.system.Txn;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL;

import de.thingweb.directory.ThingDirectory;
import de.thingweb.directory.rest.BadRequestException;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTResource;
import de.thingweb.directory.sparql.client.Connector;
import de.thingweb.directory.sparql.client.Queries;

public class VocabularyCollectionResource extends DirectoryCollectionResource {

	public VocabularyCollectionResource() {
		super("/vocab", RDFDocument.factory());
		
		try {
			Set<String> prefixes = new HashSet<>();
			
			RDFConnection conn = Connector.getConnection();
			Txn.executeRead(conn, () -> {
				Query q = Queries.listGraphs(OWL.Ontology);
				
				conn.querySelect(q, (qs) -> {
					String uri = qs.getResource("id").getURI();
					if (uri.contains(name)) {
						String id = uri.substring(uri.lastIndexOf("/") + 1);
						prefixes.add(id);
					}
				});
			});

			prefixes.forEach(name -> repost(name));
		} catch (Exception e) {
			ThingDirectory.LOG.error("Cannot fetch existing vocabularies from the RDF store", e);
		}
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
			String reason = "Registering invalid vocabulary, no instance of owl:Ontology: " + vocab;
			ThingDirectory.LOG.error(reason);
			throw new BadRequestException(reason);
		} else {
			return root;
		}
	}
	
	private boolean isRootOntology(String uri, OntModel m) {
		return !m.contains(null, OWL.imports, ResourceFactory.createResource(uri));
	}

}
