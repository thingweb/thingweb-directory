package de.thingweb.directory.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ModelFactory;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.Rio;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

import de.thingweb.directory.ThingDirectory;
import de.thingweb.directory.servlet.exception.MalformedDocumentException;
import de.thingweb.directory.sparql.client.Queries;
import de.thingweb.directory.vocabulary.TD;

public class TDServlet extends RDFDocumentServlet {
	
	protected static final String TD_CONTEXT_URI = "https://w3c.github.io/wot/w3c-wot-td-context.jsonld";
	
	protected static final String TD_FRAME_FILENAME = "td-frame.json";
	
	protected static Object TD_FRAME_OBJECT;
	
	static {
		try {
			InputStream in = TDServlet.class.getClassLoader().getResourceAsStream(TD_FRAME_FILENAME);
			TD_FRAME_OBJECT = JsonUtils.fromInputStream(in);
		} catch (IOException e) {
			ThingDirectory.LOG.error("TD frame could not be loaded from resource file", e);
			TD_FRAME_OBJECT = null;
		}
	}
	
	@Override
	protected Collection<String> getAllItems() {
		Collection<String> ids = new HashSet<String>();
		
		try {
			String pattern = String.format("?td a <%s>", TD.Thing.stringValue());
			
			// fetches items IDs from RDF store if accessible
			try (TupleQueryResult res = Queries.listResources(pattern)) {
				while (res.hasNext()) {
					String uri = res.next().getValue("res").stringValue();
					String id = getItemId(uri);
					
					ids.add(id);
				}
				
				items = ids;
			}
		} catch (Exception e) {
			ThingDirectory.LOG.error("Cannot fetch existing TDs from the RDF store", e);
		}

		return items;
	}
	
	@Override
	protected void writeContent(Model m, HttpServletRequest req, HttpServletResponse resp) throws RDFHandlerException, IOException {		
		RDFFormat format = getAcceptedFormat(req);
		
		if (format.equals(RDFFormat.JSONLD) && TD_FRAME_OBJECT != null) {
			// performs JSON-LD framing
			try {
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				Rio.write(m, buffer, format);
				
				Object obj = JsonUtils.fromString(buffer.toString());
				Object framed = JsonLdProcessor.frame(obj, TD_FRAME_OBJECT, new JsonLdOptions());
				
				resp.setContentType(RDFFormat.JSONLD.getDefaultMIMEType());
				
				Object td = getThingObject(framed);
				if (td != null) {
					JsonUtils.write(new OutputStreamWriter(resp.getOutputStream()), td);
				} else {
					ThingDirectory.LOG.warn("Framed object not as expected (TD)");
					resp.getOutputStream().write(buffer.toByteArray());
				}
			} catch (JsonLdError e) {
				ThingDirectory.LOG.error("Could not frame TD output (JSON-LD)", e);
				resp.sendError(500); // Internal Server Error
			}
		} else {
			super.writeContent(m, req, resp);
		}
	}

	@Override
	protected String generateItemID(Model m) throws MalformedDocumentException {
		Set<Resource> things = m.filter(null, RDF.TYPE, TD.Thing).subjects();
		
		Iterator<Resource> iterator = things.iterator();
		if (!iterator.hasNext()) {
			throw new MalformedDocumentException("No instance of td:Thing found in the RDF payload");
		}
		
		Resource res = iterator.next();
		String id = res instanceof IRI ? URLEncoder.encode(res.toString()) : super.generateItemID();
		
		if (iterator.hasNext()) {
			// TODO should split TD documents
		}

		return id;
	}
	
	/**
	 * 
	 * @param flattened a JSON-LD flattened object
	 * @return the first Thing object found in flattened
	 */
	@SuppressWarnings("unchecked")
	private Object getThingObject(Object flattened) {
		if (flattened instanceof Map) {
			Map<String, Object> map = (Map<String, Object>) flattened;
			Object graph = map.get("@graph");
			
			if (graph != null && graph instanceof List) {
				List<Object> list = (List<Object>) graph;
				if (!list.isEmpty()) {
					Map<String, Object> td = (Map<String, Object>) list.get(0);
					
					// TODO remove after TD spec is relaxed
					// 1. @context must be an array
					List<String> ctx = new ArrayList<>();
					ctx.add(TD_CONTEXT_URI);
					((Map<String, Object>) td).put("@context", ctx);
					// 2. @type must be an array
					processThingObject(td);
					
					return td;
				}
			}
		}
		
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private void processThingObject(Object thing) {
		if (thing instanceof Map) {
			Map<String, Object> map = (Map<String, Object>) thing;
			
			if (map.containsKey("@type") && map.get("@type") instanceof String) {
				List<String> type = new ArrayList<>();
				type.add((String) map.get("@type"));
				map.put("@type", type);
			}
			
			map.forEach((k, v) -> processThingObject(v));
		} else if (thing instanceof List) {
			List<Object> list = (List<Object>) thing;
			list.forEach(i -> processThingObject(i));
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
				if (!m.contains(node, RDF.TYPE, SimpleValueFactory.getInstance().createIRI(TD.Thing.stringValue())) && !visited.contains(node)) {
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
