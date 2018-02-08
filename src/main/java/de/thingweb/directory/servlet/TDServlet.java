package de.thingweb.directory.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.Rio;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

import de.thingweb.directory.ThingDirectory;
import de.thingweb.directory.sparql.client.Queries;
import de.thingweb.directory.vocabulary.TD;

public class TDServlet extends RDFDocumentServlet {
	
	protected static final String TD_FRAME_FILENAME = "td-frame.json";
	
	public TDServlet() {
		try {
			String pattern = String.format("?td a <%s>", TD.Thing.stringValue());
			
			try (TupleQueryResult res = Queries.listResources(pattern)) {
				while (res.hasNext()) {
					String uri = res.next().getValue("res").stringValue();
					String id = getItemId(uri);
					
					items.add(id);
				}
			}
		} catch (Exception e) {
			ThingDirectory.LOG.error("Cannot fetch existing TDs from the RDF store", e);
		}
	}
	
	@Override
	protected void writeContent(Model m, HttpServletRequest req, HttpServletResponse resp) throws RDFHandlerException, IOException {
		RDFFormat format = getAcceptedFormat(req);
		
		if (format.equals(RDFFormat.JSONLD)) {
			// performs JSON-LD framing
			try {
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				Rio.write(m, buffer, format);
				
				Object obj = JsonUtils.fromString(buffer.toString());
				// TODO cache frame object?
				Object framed = JsonLdProcessor.frame(obj, getFrame(), new JsonLdOptions());
				
				resp.setContentType(RDFFormat.JSONLD.getDefaultMIMEType());
				
				if (framed instanceof Map) {
					Object graph = ((Map<String, Object>) framed).get("@graph");
					Object td = ((List<Object>) graph).get(0);
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
	protected String generateItemID(Model m) throws ServletException {
		Set<Resource> things = m.filter(null, RDF.TYPE, SimpleValueFactory.getInstance().createIRI(TD.Thing.stringValue())).subjects();
		
		Iterator<Resource> iterator = things.iterator();
		if (!iterator.hasNext()) {
			// TODO catch it in parent code to return Bad Request
			throw new ServletException("No instance of td:Thing found in the RDF payload");
		}
		
		Resource res = iterator.next();
		String id = res instanceof IRI ? URLEncoder.encode(res.toString()) : super.generateItemID();
		
		if (iterator.hasNext()) {
			// TODO should split TD documents
		}

		return id;
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
	
	protected static Object getFrame() throws IOException {
		InputStream in = TDServlet.class.getClassLoader().getResourceAsStream(TD_FRAME_FILENAME);
		return JsonUtils.fromInputStream(in);
	}
	
}
