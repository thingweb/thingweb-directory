package de.thingweb.directory.resources;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ResponseHeader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.system.Txn;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

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
			
			Element pattern = QueryFactory.createElement("{" + q + "}");
			Query query = Queries.filterTDs(pattern);
			
			try (RDFConnection conn = Connector.getConnection(true)) {
				names = Txn.calculateRead(conn, () -> {
					Set<String> tds = new HashSet<>();
					
					conn.querySelect(query, (qs) -> {
						String uri = qs.getResource("id").getURI();
						if (uri.contains("td/")) {
							String id = uri.substring(uri.lastIndexOf("td/") + 3);
							tds.add(id);
						}
					});
					
					return tds;
				});
			}
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
			
			RDFConnection conn = Connector.getConnection();
			Txn.executeRead(conn, () -> {
				Query q = Queries.listGraphs(TD.Thing);
				
				conn.querySelect(q, (qs) -> {
					String uri = qs.getResource("id").getURI();
					if (uri.contains(name)) {
						String id = uri.substring(uri.lastIndexOf("/") + 1);
						names.add(id);
					}
				});
			});

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
		Model graph = RDFDocument.read(parameters, payload);

		List<RESTResource> resources = new ArrayList<>();
		
		if (parameters.containsKey(RESTResource.PARAMETER_CONTENT_TYPE)) {
			// forces default RDF format
			parameters.remove(RESTResource.PARAMETER_CONTENT_TYPE);
		}
		
		// TODO include inferred type statements (RDFS)
		ResIterator it = graph.listResourcesWithProperty(RDF.type, TD.Thing);
		while (it.hasNext()) {
			Resource root = it.next();
			String name = getChildID(root);

			// duplicate detection
			// TODO isomorphic TDs too
			boolean duplicate = false;
			if (root.isURIResource()) {
				String query = "ASK WHERE { GRAPH ?g { <%s> ?p ?o } }";
				RDFConnection conn = Connector.getConnection();
				duplicate = conn.queryAsk(String.format(query, root.getURI()));
			}

			if (!duplicate) {
				// TODO keyword extraction
				
				Model td = extractTD(root, new HashSet<>());
				
				// includes a reference to the Directory resource being created
				// FIXME resolve URI resolution issues...
				Resource ref = ResourceFactory.createResource("http://example.org" + path + "/" + name);
				td.add(root, RDFS.isDefinedBy, ref);
				
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				td.write(out, RDFDocument.DEFAULT_FORMAT);
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
		if (res.isURIResource()) {
			return URLEncoder.encode(res.getURI());
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
	private static Model extractTD(Resource root, Set<Resource> visited) {
		Model td = ModelFactory.createDefaultModel();
		
		visited.add(root);

		StmtIterator it = root.listProperties();
		while (it.hasNext()) {
			Statement st = it.next();
			td.add(st);
			if (!st.getPredicate().equals(RDF.type) && st.getObject().isResource()) {
				Resource node = st.getObject().asResource();
				if (!node.hasProperty(RDF.type, TD.Thing) && !visited.contains(node)) {
					td.add(extractTD(node, visited));
				}
			}
		}
		  
		return td;
	}

}
