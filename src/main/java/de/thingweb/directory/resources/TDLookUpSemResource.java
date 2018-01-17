package de.thingweb.directory.resources;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.system.Txn;

import de.thingweb.directory.ThingDirectory;
import de.thingweb.directory.rest.BadRequestException;
import de.thingweb.directory.rest.CollectionFilter;
import de.thingweb.directory.rest.CollectionFilterFactory;
import de.thingweb.directory.rest.CollectionResource;
import de.thingweb.directory.rest.MethodNotAllowedException;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTResource;
import de.thingweb.directory.sparql.client.Connector;
import de.thingweb.directory.sparql.client.Queries;

public class TDLookUpSemResource extends DirectoryCollectionResource {
	
	public static final String PARAMETER_QUERY = "query";
	
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
		
	}

	public TDLookUpSemResource() {
		super("/td-lookup/sem", TDResource.factory(), new CollectionFilterFactory() {
			@Override
			public CollectionFilter create(Map<String, String> parameters) throws BadRequestException {
				try {
					if (parameters.containsKey(PARAMETER_QUERY)) {
						String q = parameters.get(PARAMETER_QUERY);
						return new SPARQLFilter(q);
					} else {
						return new CollectionResource.KeepAllFilter();
					}
				} catch (BadRequestException e) {
					throw e;
				}
			}
		});
	}
	
	@Override
	public void get(Map<String, String> parameters, OutputStream out) throws RESTException {
		super.get(parameters, out);

		// TODO include TDs
	}
	
	@Override
	public RESTResource post(Map<String, String> parameters, InputStream payload) throws RESTException {
		throw new MethodNotAllowedException();
	}
	
}
