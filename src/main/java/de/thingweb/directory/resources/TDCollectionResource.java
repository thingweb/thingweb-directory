package de.thingweb.directory.resources;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ResponseHeader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.vocabulary.RDF;

import de.thingweb.directory.ThingDirectory;
import de.thingweb.directory.VocabularyUtils;
import de.thingweb.directory.rest.BadRequestException;
import de.thingweb.directory.rest.CollectionResource;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTResource;
import de.thingweb.directory.vocabulary.TD;

@Api(value = "thing_description")
public class TDCollectionResource extends CollectionResource {

	public TDCollectionResource() {
		super("/td", TDResource.factory());
		
		// TODO create child resources for all graphs already in the RDF store.
	}

	@Override
	@ApiOperation(value = "Lists all TDs in the repository.",
	              httpMethod = "GET",
	              produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "query",
	                      value = "SPARQL graph pattern (URI-encoded)"),
		@ApiImplicitParam(name = "text",
		                  value = "Keyword for boolean text search query")
	})
	public void get(Map<String, String> parameters, OutputStream out) throws RESTException {
		super.get(parameters, out);

		// TODO include TDs?
		
		// TODO SPARQL querying, free text search
	}
	
	@Override
	@ApiOperation(value = "Creates (adds) a TD to the repository.",
	              httpMethod = "POST",
	              consumes = "application/ld+json, application/rdf+xml, text/turtle, application/n-triples",
	              responseHeaders = @ResponseHeader(name = "Location",
	                                                description = "Relative URI to the created resource"))
	public RESTResource post(Map<String, String> parameters, InputStream payload) throws RESTException {
		Model graph = RDFDocument.read(parameters, payload);
		Model schema = VocabularyUtils.mergeVocabularies();
		// FIXME reasoning on the union dataset!
		InfModel inf = ModelFactory.createInfModel(ReasonerRegistry.getOWLMicroReasoner(), schema, graph);

		List<RESTResource> resources = new ArrayList<>();
		
		if (parameters.containsKey(RESTResource.PARAMETER_CONTENT_TYPE)) {
			// forces default RDF format
			parameters.remove(RESTResource.PARAMETER_CONTENT_TYPE);
		}
		
		ResIterator it = inf.listResourcesWithProperty(RDF.type, TD.Thing);
		while (it.hasNext()) {
			Resource root = it.next();

			// duplicate detection
			// TODO isomorphic TDs too
			boolean duplicate = false;
			if (root.isURIResource()) {
				String query = "ASK WHERE { GRAPH <%s> { ?s ?p ?o } }";
				RDFConnection conn = ThingDirectory.get().getStoreConnection();
				duplicate = conn.queryAsk(String.format(query, root.getURI()));
			}

			if (!duplicate) {
				// TODO keyword extraction
				
				Model td = extractTD(root);
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				td.write(out, RDFDocument.DEFAULT_FORMAT);
				ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
				
				RESTResource res = super.post(parameters, in);
				resources.add(res);
			} else {
				ThingDirectory.LOG.info("Registering invalid TD, no instance of td:Thing: " + graph);
			}
		}
		
		if (resources.isEmpty()) {
			ThingDirectory.LOG.info("Registering invalid TD, no instance of td:Thing: " + graph);
			throw new BadRequestException();
		} else {
			return resources.get(0);
		}
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
