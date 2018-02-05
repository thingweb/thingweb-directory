package de.thingweb.directory.resources;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

import de.thingweb.directory.ThingDirectory;
import de.thingweb.directory.rest.RESTException;
import de.thingweb.directory.rest.RESTResource;
import de.thingweb.directory.rest.RESTResourceFactory;

public class TDResource extends RDFDocument {
	
	protected static final String TD_FRAME_FILENAME = "td-frame.json";
	
	public TDResource(String path) {
		this(path, new HashMap<String, String>());
	}
	
	public TDResource(String path, Map<String, String> parameters) {
		super(path, parameters);
	}
	
	public TDResource(String path, InputStream in) {
		this(path, new HashMap<>(), in);
	}
	
	public TDResource(String path, Map<String, String> parameters, InputStream in) {
		super(path, parameters, in);
	}
	
	@Override
	public void get(Map<String, String> parameters, OutputStream out) throws RESTException {
		// TODO remove unnecessary buffering step (use a request life cycle model instead)
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		super.get(parameters, buffer);
		
		RDFFormat format = RDFDocument.getOutputAccept(parameters);
		if (format.equals(RDFFormat.JSONLD)) {
			// requires pre-processing: JSON-LD document returned flat from RDF store
			try {
				Object obj = JsonUtils.fromString(buffer.toString());
				Object frame = getFrame();
				
				Object framed = JsonLdProcessor.frame(obj, frame, new JsonLdOptions());
				
				if (framed instanceof Map) {
					Object graph = ((Map<String, Object>) framed).get("@graph");
					Object td = ((List<Object>) graph).get(0);
					JsonUtils.write(new OutputStreamWriter(out), td);
				} else {
					ThingDirectory.LOG.warn("Framed object not as expected (TD)");
				}
			} catch (IOException | JsonLdError e) {
				ThingDirectory.LOG.error("Could not frame TD output (JSON-LD)", e);
				throw new RESTException(e);
			}
		} else {
			try {
				out.write(buffer.toByteArray());
			} catch (IOException e) {
				ThingDirectory.LOG.error("Could not read TD buffer", e);
				throw new RESTException(e);
			}
		}
	}
	
	public static RESTResourceFactory factory() {
		return new RESTResourceFactory() {
			
			@Override
			public RESTResource create(String path) {
				return new TDResource(path);
			}
			
			@Override
			public RESTResource create(String path, Map<String, String> parameters) {
				return new TDResource(path, parameters);
			}
			
			@Override
			public RESTResource create(String path, InputStream payload) {
				return new TDResource(path, payload);
			}
			
			@Override
			public RESTResource create(String path, Map<String, String> parameters, InputStream payload) {
				return new TDResource(path, parameters, payload);
			}
			
		};
	}
	
	protected static Object getFrame() throws IOException {
		InputStream in = TDResource.class.getClassLoader().getResourceAsStream(TD_FRAME_FILENAME);
		return JsonUtils.fromInputStream(in);
	}

}
