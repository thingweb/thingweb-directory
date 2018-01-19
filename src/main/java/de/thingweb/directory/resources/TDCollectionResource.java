package de.thingweb.directory.resources;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ResponseHeader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.Syntax;
import org.apache.jena.sparql.syntax.Element;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;

import de.thingweb.directory.ThingDirectory;
import de.thingweb.directory.rest.BadRequestException;
import de.thingweb.directory.rest.CollectionFilter;
import de.thingweb.directory.rest.CollectionFilterFactory;
import de.thingweb.directory.rest.CollectionResource;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTResource;
import de.thingweb.directory.sparql.client.Connector;
import de.thingweb.directory.sparql.client.Queries;
import de.thingweb.directory.vocabulary.TD;

@Api(value = "thing_description")
public class TDCollectionResource extends DirectoryCollectionResource {
	
	public static final String PARAMETER_QUERY = "query";
	
	public static final String PARAMETER_TEXT_SEARCH = "text";
	
	private static class SPARQLFilter implements CollectionFilter {
		
		private final Set<String> names;
		
		public SPARQLFilter(String q) throws BadRequestException {
			if (!q.contains("?thing")) {
				String reason = "SPARQL filter does not contain the mandatory ?thing variable";
				ThingDirectory.LOG.info(reason);
				throw new BadRequestException(reason);
			}
			
			HashSet<String> tds = new HashSet<>();
			
			Element pattern = QueryFactory.createElement("{" + q + "}");
			String select = Queries.filterTDs(pattern).toString(Syntax.syntaxSPARQL_11);
			
			RepositoryConnection conn = Connector.getRepositoryConnection();
			TupleQuery query = conn.prepareTupleQuery(select);
			
			try (TupleQueryResult res = query.evaluate()) {
				while (res.hasNext()) {
					String uri = res.next().getValue("id").stringValue();
					if (uri.contains("td/")) {
						String id = uri.substring(uri.lastIndexOf("td/") + 3);
						tds.add(id);
					}
				}
			}
			
			names = tds;
		}
		
		@Override
		public boolean keep(RESTResource child) {
			return names.contains(child.getName());
		}
		
		public Set<String> getNames() {
			return names;
		}
		
	}
	
	private static class FreeTextFilter implements CollectionFilter {
		
		private final String keywords;

		public FreeTextFilter(String kw) {
			keywords = kw;
		}
		
		@Override
		public boolean keep(RESTResource child) {
			return true; // TODO
		}
		
	}

	public TDCollectionResource() {
		super("/td", TDResource.factory(), new CollectionFilterFactory() {
			@Override
			public CollectionFilter create(Map<String, String> parameters) throws BadRequestException {
				try {
					if (parameters.containsKey(PARAMETER_QUERY)) {
						String q = parameters.get(PARAMETER_QUERY);
						return new SPARQLFilter(q);
					} else if (parameters.containsKey(PARAMETER_TEXT_SEARCH)) {
						String keywords = parameters.get(PARAMETER_TEXT_SEARCH);
						return new FreeTextFilter(keywords);
					} else {
						return new CollectionResource.KeepAllFilter();
					}
				} catch (BadRequestException e) {
					throw e;
				}
			}
		});
		
		try {
			Set<String> names = new HashSet<>();
			
			RepositoryConnection conn = Connector.getRepositoryConnection();
			String select = Queries.listGraphs(TD.Thing).toString(Syntax.syntaxSPARQL_11);
			TupleQuery q = conn.prepareTupleQuery(select);
			
			try (TupleQueryResult res = q.evaluate()) {
				while (res.hasNext()) {
					String uri = res.next().getValue("id").stringValue();
					
					if (uri.contains(name)) {
						String id = uri.substring(uri.lastIndexOf("/") + 1);
						names.add(id);
					}
				}
			}

			names.forEach(name -> repost(name));
		} catch (Exception e) {
			ThingDirectory.LOG.error("Cannot fetch existing TDs from the RDF store", e);
		}
	}

	@ApiOperation(value = "Lists all TDs in the repository.",
	              httpMethod = "GET",
	              produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "query",
	                      value = "SPARQL graph pattern (URI-encoded)"),
		@ApiImplicitParam(name = "text",
		                  value = "Keyword for boolean text search query")
	})
	@Override
	public void get(Map<String, String> parameters, OutputStream out) throws RESTException {
		super.get(parameters, out);

		// TODO include TDs?
	}
	
	@ApiOperation(value = "Creates (adds) a TD to the repository.",
	              httpMethod = "POST",
	              consumes = "application/ld+json, application/rdf+xml, text/turtle, application/n-triples",
	              responseHeaders = @ResponseHeader(name = "Location",
	                                                description = "Relative URI to the created resource"))
	@Override
	public RESTResource post(Map<String, String> parameters, InputStream payload) throws RESTException {
		Model graph;
		try {
			graph = RDFDocument.read(parameters, payload);
		} catch (RDFParseException | UnsupportedRDFormatException | IOException e) {
			throw new BadRequestException(e);
		}

		List<RESTResource> resources = new ArrayList<>();
		
		if (parameters.containsKey(RESTResource.PARAMETER_CONTENT_TYPE)) {
			// forces default RDF format
			parameters.remove(RESTResource.PARAMETER_CONTENT_TYPE);
		}
		
		// TODO RDFS inference to get all td:Things
		Set<Resource> things = graph.filter(null, RDF.TYPE, SimpleValueFactory.getInstance().createIRI(TD.Thing.getURI())).subjects();
		for (Resource root : things) {
			String name = getChildID(root);

			// duplicate detection
			// TODO isomorphic TDs too
			boolean duplicate = false;
			if (root instanceof IRI) {
				RepositoryConnection conn = Connector.getRepositoryConnection();
				String ask = String.format("ASK WHERE { GRAPH ?g { <%s> ?p ?o } }", root);
				BooleanQuery q = conn.prepareBooleanQuery(ask);
				duplicate = q.evaluate();
			}
			
			if (!duplicate) {
				Model td = extractTD(graph, root, new HashSet<>());
				
				// includes a reference to the Directory resource being created
				Resource ref = SimpleValueFactory.getInstance().createIRI(ThingDirectory.getBaseURI() + path + "/" + name);
				td.add(root, RDFS.ISDEFINEDBY, ref);
				
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				Rio.write(td, out, RDFDocument.DEFAULT_RDF_FORMAT);
				ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());

				// child resource ID will be set to 'name'
				idQueue.add(name);
				
				RESTResource res = super.post(parameters, in);
				resources.add(res);
			} else {
				for (RESTResource res : children) {
					if (res.getName().equals(name)) {
						// TODO will return 201 but it shouldn't
						resources.add(res);
					}
				}
				ThingDirectory.LOG.info("TD already registered: " + root);
			}
		}
		
		if (resources.isEmpty()) {
			String reason = "Registering invalid TD, no instance of td:Thing: " + graph;
			ThingDirectory.LOG.info(reason);
			throw new BadRequestException(reason);
		} else {
			return resources.get(0);
		}
	}
	
	private String getChildID(Resource res) {
		if (res instanceof IRI) {
			return URLEncoder.encode(res.toString());
		} else {
			return super.generateChildID();
		}
	}
	
	/**
	 * breadth-first traversal of the RDF model
	 * 
	 * @param root starting point of the traversal
	 * @param visited set of visited nodes (should be empty)
	 * @return
	 */
	private static Model extractTD(Model m, Resource root, Set<Resource> visited) {
		Model td = new ModelBuilder().build();
		
		visited.add(root);
		
		m.filter(root, null, null).forEach(stm -> {
			IRI p = stm.getPredicate();
			Value o = stm.getObject();
			td.add(stm);
			if (!p.equals(RDF.TYPE) && o instanceof Resource) {
				Resource node = (Resource) o;
				if (!m.contains(node, RDF.TYPE, SimpleValueFactory.getInstance().createIRI(TD.Thing.getURI())) && !visited.contains(node)) {
					Model submodel = extractTD(m, node, visited);
					submodel.forEach(substm -> {
						td.add(substm);
					});
				}
			}
		});
		
		return td;
	}

}
