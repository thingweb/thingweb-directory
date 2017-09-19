package de.thingweb.directory.handlers;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.apache.jena.atlas.lib.StrUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.util.QueryExecUtils;

import de.thingweb.directory.ThingDescriptionUtils;
import de.thingweb.directory.rest.BadRequestException;
import de.thingweb.directory.rest.NotFoundException;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTHandler;
import de.thingweb.directory.rest.RESTResource;
import de.thingweb.directory.rest.RESTServerInstance;
import de.thingweb.directory.ThingDirectory;

public class TDLookUpSEMHandler extends RESTHandler {

	public TDLookUpSEMHandler(List<RESTServerInstance> instances) {
		super("sem", instances);
	}

	@Override
	public RESTResource get(URI uri, Map<String, String> parameters) throws RESTException {

		RESTResource resource = new RESTResource(name(uri), this);
		resource.contentType = "application/ld+json";
		resource.content = "{";
		
		List<String> tds = new ArrayList<String>();
		String query;
		
		// Normal SPARQL query
		if (parameters.containsKey("query") && !parameters.get("query").isEmpty()) {
			
			query = parameters.get("query");
			try {
				tds = ThingDescriptionUtils.listThingDescriptions(query);
			} catch (Exception e) {
				throw new BadRequestException();
			}
			
		} else if (parameters.containsKey("text") && !parameters.get("text").isEmpty()) { // Full text search query
			
			query = parameters.get("text");

			try {
				tds = ThingDescriptionUtils.listThingDescriptionsFromTextSearch(query);
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("Unable to retrieve Thing Description ");
				throw new BadRequestException();
			}
			
		} else if (parameters.containsKey("rdf") && !parameters.get("rdf").isEmpty()) { // RDF type/value type query
			
			query = parameters.get("rdf");
			try {
				tds = ThingDescriptionUtils.listRDFTypeValues(query);
			} catch (Exception e) {
				throw new BadRequestException();
			}
			
			// Retrieve type values
			for (int i = 0; i < tds.size(); i++) {
				resource.content += "\"unit\": " + tds.get(i);
				if (i < tds.size() - 1) {
					resource.content += ",\n";
				}
			}
			
			resource.content += "}";
			return resource;
			
		} else {
			// Return all TDs
			try {
				tds = ThingDescriptionUtils.listThingDescriptions("?s ?p ?o");
			} catch (Exception e) {
				throw new BadRequestException();
			}
		}
		
		// Retrieve Thing Description(s)
		for (int i = 0; i < tds.size(); i++) {
			URI td = URI.create(tds.get(i));
			
			try {
				ThingDescriptionHandler h = new ThingDescriptionHandler(td.toString(), instances);
				RESTResource res = h.get(td, new HashMap<String, String>());
				// TODO check TD's content type
				
				resource.content += "\"" + td.getPath() + "\": " + res.content;
				if (i < tds.size() - 1) {
					resource.content += ",";
				}
				
			} catch (NotFoundException e) {
				// remove ","
				if (resource.content.endsWith(",")) {
					resource.content = resource.content.substring(0, resource.content.length() -1);
				}
				continue; // Life time is invalid and TD was removed
				
			} catch (RESTException e) {
				// remove ","
				if (resource.content.endsWith(",")) {
					resource.content = resource.content.substring(0, resource.content.length() -1);
				}
				continue; // Life time is invalid and TD was removed
				
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("Unable to retrieve Thing Description " + td.getPath());
			}
		}
		
		resource.content += "}";
		return resource;
	}
	
		@Override
	public RESTResource post(URI uri, Map<String, String> parameters, InputStream payload) throws RESTException {
		String data = "";
		List<String> tds = new ArrayList<>();
		try {
			data = ThingDescriptionUtils.streamToString(payload);
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new BadRequestException();
		}

		String resultTriples = new String();
		RESTResource resource = new RESTResource(uri.toString(), this);
		resource.contentType = "application/ld+json";
		resource.content = "{";	

		try {
			if (data.contains("graph") || !data.contains(";")) {

				if (data.contains(";")){
					data = data.substring(data.indexOf(";") + 1);
				}
               tds = ThingDescriptionUtils.listThingDescriptions(data);
			  
                // Retrieve Thing Description(s)
				for (int i = 0; i < tds.size(); i++) {
					URI td = URI.create(tds.get(i));
					ThingDescriptionHandler h = new ThingDescriptionHandler(td.toString(), instances);
					RESTResource res = h.get(td, new HashMap<String, String>());
					// TODO check TD's content type

					resource.content += "\"" + td.getPath() + "\": " + res.content;
					if (i < tds.size() - 1) {
						resource.content += ",";
					}
				}
			} else if (data.contains("triple")) {
				data = data.substring(data.indexOf(";")+1);
				Dataset dataset = ThingDirectory.get().dataset;
		        dataset.begin(ReadWrite.READ);
				String q = "SELECT DISTINCT " + data;
				QueryExecution qexec = QueryExecutionFactory.create(q, dataset);
				ResultSet result = qexec.execSelect();
				int k = 1;
				while (result.hasNext()) {
					QuerySolution res = result.next();
					resource.content += "\"Triple" + k++ + "\": " + res.toString();
					resource.content += ",";
				}
				dataset.end();
			}
			int length = (resource.content).length();
			if(length>2)
			resource.content = resource.content.substring(0,length-2) ;
			resource.content += "}";
			return resource;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resource;
	}
	
	private String name(URI uri) {
		
		String path = uri.getPath();
		if (path.contains("/")) {
			return path.substring(uri.getPath().lastIndexOf("/") + 1);
		}
		return path;
	}
}