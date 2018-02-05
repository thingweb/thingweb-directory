package de.thingweb.directory.resources;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.jena.query.Syntax;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;

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
			
			String pattern = String.format("?vocab a <%s>", OWL.ONTOLOGY);
			
			try (TupleQueryResult res = Queries.listResources(pattern)) {
				while (res.hasNext()) {
					String uri = res.next().getValue("res").stringValue();
					
					if (uri.contains(name)) {
						String id = uri.substring(uri.lastIndexOf("/") + 1);
						prefixes.add(id);
					}
				}
			}

			prefixes.forEach(name -> repost(name));
		} catch (Exception e) {
			ThingDirectory.LOG.error("Cannot fetch existing vocabularies from the RDF store", e);
		}
	}
	
	@Override
	public RESTResource post(Map<String, String> parameters, InputStream payload) throws RESTException {
		Model vocab;
		try {
			String base = RDFDocument.getInputBaseURI(parameters);
			RDFFormat format = RDFDocument.getInputContentType(parameters);
			vocab = Rio.parse(payload, base, format);
		} catch (RDFParseException | UnsupportedRDFormatException | IOException e) {
			throw new BadRequestException(e);
		}

		RESTResource root = null;
		
		if (parameters.containsKey(RESTResource.PARAMETER_CONTENT_TYPE)) {
			// forces default RDF format
			parameters.remove(RESTResource.PARAMETER_CONTENT_TYPE);
		}
		
		Set<Resource> ontologies = vocab.filter(null, RDF.TYPE, OWL.ONTOLOGY).subjects();
		for (Resource o : ontologies) {
			// TODO get namespace prefix
			String id = generateChildID();
			
			// TODO fetch imports
			Model axioms = vocab;

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			Rio.write(axioms, out, RDFDocument.DEFAULT_RDF_FORMAT);
			ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());

			RESTResource res = super.post(parameters, in);
			root = res;

			ThingDirectory.LOG.info(String.format("Registered RDFS/OWL vocabulary %s (id: %s)", o, id));
		}

		if (root == null) {
			String reason = "Registering invalid vocabulary, no instance of owl:Ontology: " + vocab;
			ThingDirectory.LOG.error(reason);
			throw new BadRequestException(reason);
		} else {
			return root;
		}
	}

}
