package de.thingweb.directory.rest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.thingweb.directory.ThingDirectory;

/**
 * Note: this resource cannot be deleted. It is the responsibility of the subclasses
 * allowing the DELETE operation to notify listeners.
 *
 * @author z003dp6d
 * @creation 28.09.2017
 *
 */
public class RESTResource {
	
	/**
	 * Note: some headers should also be given as parameters, e.g. to allow for content negotiation (Accept/Content-Type).
	 */
	public final static String PARAMETER_ACCEPT = "accept";
	public final static String PARAMETER_CONTENT_TYPE = "ct";
	
	protected final Set<RESTResourceListener> listeners = new HashSet<>();
	
	protected final String name;
	protected final String path;
	
	protected String content = null;
	protected String contentType = "text/plain";
	
	public RESTResource(String path) {
		this(path, new HashMap<>(), null);
	}
	
	public RESTResource(String path, Map<String, String> parameters) {
		this(path, parameters, null);
	}
	
	public RESTResource(String path, InputStream in) {
		this(path, new HashMap<>(), in);
	}
	
	public RESTResource(String path, Map<String, String> parameters, InputStream in) {
		this.path = path;
		if (path.contains("/")) {
		  this.name = path.substring(path.lastIndexOf('/') + 1);
		} else {
		  this.name = path;
		}
		
		if (parameters.containsKey(PARAMETER_CONTENT_TYPE)) {
			contentType = parameters.get(PARAMETER_CONTENT_TYPE);
		}
		
		if (in != null) {
			try {
				content = streamToString(in);
			} catch (IOException e) {
				ThingDirectory.LOG.error("Cannot store resource content as UTF-8 string", e);
			}
		}
	}

	public String getPath() {
		return path;
	}
	
	public String getName() {
		return name;
	}
	
	public String getContentType() {
		return contentType;
	}
	
	public void setContentType(String ct) {
		contentType = ct;
	}
	
	public void addListener(RESTResourceListener listener) {
		listeners.add(listener);
	}
	
	public void removeListener(RESTResourceListener listener) {
		listeners.remove(listener);
	}
	
	public void get(Map<String, String> parameters, OutputStream out) throws RESTException {
		if (content == null) {
			try {
				out.close();
			} catch (IOException e) {
				ThingDirectory.LOG.error("Cannot send empty content", e);
				throw new RESTException();
			}
			return;
		}

		try {
			out.write(content.getBytes());
			out.close();
		} catch (IOException e) {
			ThingDirectory.LOG.error("Cannot send resource content (UTF-8)", e);
			throw new RESTException();
		}
	}
	
	public RESTResource post(Map<String, String> parameters, InputStream payload) throws RESTException {
		throw new MethodNotAllowedException();
	}
	
	public void put(Map<String, String> parameters, InputStream payload) throws RESTException {
		throw new MethodNotAllowedException();
	}
	
	public void delete(Map<String, String> parameters) throws RESTException {
		throw new MethodNotAllowedException();
	}
	
	public void observe(Map<String, String> parameters, OutputStream out) throws RESTException {
		throw new MethodNotAllowedException();
	}

	protected String streamToString(InputStream s) throws IOException {
		StringWriter w = new StringWriter();
		InputStreamReader r = new InputStreamReader(s, "UTF-8");
		char[] buf = new char[512];
		int len;

		while ((len = r.read(buf)) > 0) {
			w.write(buf, 0, len);
		}
		s.close();

		return w.toString();
	}
	
	public static RESTResourceFactory factory() {
		return new RESTResourceFactory() {
			
			@Override
			public RESTResource create(String path) {
				return new RESTResource(path);
			}
			
			@Override
			public RESTResource create(String path, InputStream payload) {
				return new RESTResource(path, payload);
			}
			
			@Override
			public RESTResource create(String path, Map<String, String> parameters, InputStream payload) {
				return new RESTResource(path, parameters, payload);
			}
			
			@Override
			public RESTResource create(String path, Map<String, String> parameters) {
				return new RESTResource(path, parameters);
			}
		};
	}
	
}
