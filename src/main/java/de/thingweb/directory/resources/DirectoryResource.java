package de.thingweb.directory.resources;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;









import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.datatypes.xsd.impl.XSDDateTimeType;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.sparql.function.library.leviathan.factorial;
import org.apache.jena.sparql.modify.request.QuadDataAcc;
import org.apache.jena.sparql.modify.request.UpdateDataInsert;
import org.apache.jena.sparql.modify.request.UpdateDeleteInsert;
import org.apache.jena.system.Txn;
import org.apache.jena.update.Update;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.XSD;
import org.eclipse.californium.core.coap.OptionNumberRegistry.optionFormats;
import org.joda.time.DateTime;









import de.thingweb.directory.ThingDescriptionUtils;
import de.thingweb.directory.ThingDirectory;
import de.thingweb.directory.rest.BadRequestException;
import de.thingweb.directory.rest.NotFoundException;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTResource;
import de.thingweb.directory.rest.RESTResourceFactory;
import de.thingweb.directory.rest.RESTResourceListener;
import de.thingweb.directory.sparql.client.Queries;

/**
 * 
 * Implements CoRE Resource Directory features, such as resource lifetime
 *
 * @author Victor Charpenay
 * @creation 22.09.2017
 *
 */
public class DirectoryResource extends RESTResource {

	/**
	 * Default lifetime: 24h (86,400s)
	 */
	public final static Integer DEFAULT_LIFETIME = 86400;
	
	public static final String PARAMETER_LIFETIME = "lt";
	
	// TODO process endpoint query parameter
	public static final String PARAMETER_ENDPOINT = "ep";
	
	protected final String uri;
	protected final Integer lifetime;
	
	public DirectoryResource(String path) {
		this(path, new HashMap<>(), null);
	}
	
	public DirectoryResource(String path, InputStream in) {
		this(path, new HashMap<>(), in);
	}
	
	public DirectoryResource(String path, Map<String, String> parameters) {
		this(path, parameters, null);
	}
	
	public DirectoryResource(String path, Map<String, String> parameters, InputStream in) {
		super(path, parameters, in);

		Integer lt = DEFAULT_LIFETIME;
		if (parameters.containsKey(PARAMETER_LIFETIME)) {
			lt = Integer.parseInt(parameters.get(PARAMETER_LIFETIME));
		}
		
		uri = ThingDirectory.get().getBaseURI() + path;
		lifetime = lt;
		
		updateTimeout(uri, true);
	}
	
	@Override
	public void get(Map<String, String> parameters, OutputStream out) throws RESTException {		
		if (hasExpired(uri)) {
			// resource is out-dated
			delete(parameters);
			throw new NotFoundException();
		}
		
		super.get(parameters, out);
	}
	
	@Override
	public void put(Map<String, String> parameters, InputStream payload) throws RESTException {
		updateTimeout(uri, false);
	}
	
	@Override
	public void delete(Map<String, String> parameters) throws RESTException {
		// TODO delete triples in RDF store?
		
		for (RESTResourceListener l : listeners) {
			l.onDelete(this);
		}
	}
	
	private boolean hasExpired(String uri) {
		Resource res = ResourceFactory.createResource(uri);
		
		try (RDFConnection conn = ThingDirectory.get().getStoreConnection()) {
			return Txn.calculateRead(conn, () -> {
				Query q = Queries.hasExpired(res);
				return conn.queryAsk(q);
			});
		}
	}
	
	private void updateTimeout(String uri, boolean firstTime) {
		Resource res = ResourceFactory.createResource(uri);
		
		try (RDFConnection conn = ThingDirectory.get().getStoreConnection()) {
			Txn.executeWrite(conn, () -> {
				Update up = firstTime ? Queries.createTimeout(res, lifetime) : Queries.updateTimeout(res, lifetime);
				conn.update(up);
			});
		}
	}
	
	public static RESTResourceFactory factory() {
		return new RESTResourceFactory() {
			
			@Override
			public RESTResource create(String path) {
				return new DirectoryResource(path);
			}
			
			@Override
			public RESTResource create(String path, InputStream payload) {
				return new DirectoryResource(path, payload);
			}
			
			@Override
			public RESTResource create(String path, Map<String, String> parameters, InputStream payload) {
				return new DirectoryResource(path, parameters, payload);
			}
			
			@Override
			public RESTResource create(String path, Map<String, String> parameters) {
				return new DirectoryResource(path, parameters);
			}
		};
	}

}
