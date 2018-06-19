/********************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
 * 
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the W3C Software Notice and
 * Document License (2015-05-13) which is available at
 * https://www.w3.org/Consortium/Legal/2015/copyright-software-and-document.
 * 
 * SPDX-License-Identifier: EPL-2.0 OR W3C-20150513
 ********************************************************************************/
package org.eclipse.thingweb.directory.coap;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.ReadListener;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.coap.CoAP.Code;
import org.eclipse.californium.core.coap.OptionNumberRegistry;
import org.eclipse.californium.core.coap.Request;

public class CoAPServletRequest implements HttpServletRequest {
	
	private class ByteArrayServletInputStream extends ServletInputStream {
		
		private final ByteArrayInputStream in;
		
		public ByteArrayServletInputStream(byte[] buffer) {
			in = new ByteArrayInputStream(buffer);
		}
		
		@Override
		public boolean isFinished() {
			// FIXME manage block-wise transfer
			return true;
		}
		
		@Override
		public boolean isReady() {
			return true;
		}
		
		@Override
		public int read() throws IOException {
			return in.read();
		}
		
		@Override
		public void setReadListener(ReadListener arg0) {
			// TODO ?
		}
		
	}
	
	private class IteratorEnumeration<T> implements Enumeration<T> {

		Iterator<T> iterator;
		
		public IteratorEnumeration(Iterator<T> it) {
			iterator = it;
		}
		
		@Override
		public boolean hasMoreElements() {
			return iterator.hasNext();
		}

		@Override
		public T nextElement() {
			return iterator.next();
		}
		
	}
	
	private final Request request;
	
	private final Map<String, Set<String>> headers;
	
	private final Map<String, String[]> parameters;
	
	public CoAPServletRequest(Request req) {
		request = req;
		headers = CoAPServletUtils.toHeaders(request.getOptions());
		parameters = CoAPServletUtils.toParameters(request.getOptions().getUriQuery());
	}

	@Override
	public AsyncContext getAsyncContext() {
		return null;
	}

	@Override
	public Object getAttribute(String arg0) {
		return null;
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return null;
	}

	@Override
	public String getCharacterEncoding() {
		return "UTF-8";
	}

	@Override
	public int getContentLength() {
		return request.getPayloadSize();
	}

	@Override
	public long getContentLengthLong() {
		return request.getPayloadSize();
	}

	@Override
	public String getContentType() {
		int format = request.getOptions().getContentFormat();
		return CoAPServletUtils.toContentType(format);
	}

	@Override
	public DispatcherType getDispatcherType() {
		return null;
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		return new ByteArrayServletInputStream(request.getPayload());
	}

	@Override
	public String getLocalAddr() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getLocalName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getLocalPort() {
		return request.getDestinationPort();
	}

	@Override
	public Locale getLocale() {
		return Locale.GERMANY;
	}

	@Override
	public Enumeration<Locale> getLocales() {
		Set<Locale> locales = new HashSet<>();
		locales.add(Locale.GERMANY);
		return new IteratorEnumeration<>(locales.iterator());
	}

	@Override
	public String getParameter(String name) {
		return parameters.get(name)[0];
	}

	@Override
	public Map<String, String[]> getParameterMap() {		
		return parameters;
	}

	@Override
	public Enumeration<String> getParameterNames() {
		return new IteratorEnumeration<>(parameters.keySet().iterator());
	}

	@Override
	public String[] getParameterValues(String name) {
		return parameters.get(name);
	}

	@Override
	public String getProtocol() {
		return "CoAP";
	}

	@Override
	public BufferedReader getReader() throws IOException {
		Reader buf = new StringReader(request.getPayloadString());
		return new BufferedReader(buf);
	}

	@Override
	public String getRealPath(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRemoteAddr() {
		return request.getSource().getHostAddress();
	}

	@Override
	public String getRemoteHost() {
		return request.getSource().getHostName();
	}

	@Override
	public int getRemotePort() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		return null;
	}

	@Override
	public String getScheme() {
		return request.getScheme();
	}

	@Override
	public String getServerName() {
		return request.getOptions().getUriHost();
	}

	@Override
	public int getServerPort() {
		return request.getDestinationPort();
	}

	@Override
	public ServletContext getServletContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAsyncStarted() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isAsyncSupported() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSecure() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeAttribute(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setAttribute(String arg0, Object arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setCharacterEncoding(String encoding) throws UnsupportedEncodingException {
		if (encoding.toLowerCase().equals("utf-8")) {
			throw new UnsupportedEncodingException();
		}
	}

	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1)
			throws IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean authenticate(HttpServletResponse arg0) throws IOException, ServletException {
		throw new ServletException("Not implemented");
	}

	@Override
	public String changeSessionId() {
		return null;
	}

	@Override
	public String getAuthType() {
		return null;
	}

	@Override
	public String getContextPath() {
		return getServletPath().replace("/$", "");
	}

	@Override
	public Cookie[] getCookies() {
		return null;
	}

	@Override
	public long getDateHeader(String name) {
		return Long.parseLong(getHeader(name));
	}

	@Override
	public String getHeader(String name) {
		return headers.get(name).iterator().next();
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		return new IteratorEnumeration<>(headers.keySet().iterator());
	}

	@Override
	public Enumeration<String> getHeaders(String name) {
		return new IteratorEnumeration<>(headers.get(name).iterator());
	}

	@Override
	public int getIntHeader(String name) {
		return Integer.parseInt(getHeader(name));
	}

	@Override
	public String getMethod() {
		return request.getCode().toString();
	}

	@Override
	public Part getPart(String arg0) throws IOException, ServletException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPathInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPathTranslated() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getQueryString() {
		return request.getOptions().getUriQueryString();
	}

	@Override
	public String getRemoteUser() {
		return null;
	}

	@Override
	public String getRequestURI() {
		// TODO test method
		String scheme = getScheme();
		String host = getServerName();
		String port = Integer.toString(getServerPort());
		String path = getServletPath();
		String query = getQueryString();
		return scheme + "://" + host + (port == null ? "" : ":" + port) +
			path + (query == null ? "" : "?" + query);
	}

	@Override
	public StringBuffer getRequestURL() {
		return new StringBuffer(getRequestURI());
	}

	@Override
	public String getRequestedSessionId() {
		return null;
	}

	@Override
	public String getServletPath() {
		return request.getOptions().getUriPathString();
	}

	@Override
	public HttpSession getSession() {
		return null;
	}

	@Override
	public HttpSession getSession(boolean create) {
		return null;
	}

	@Override
	public Principal getUserPrincipal() {
		return null;
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromUrl() {
		return false;
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		return false;
	}

	@Override
	public boolean isUserInRole(String role) {
		return false;
	}

	@Override
	public void login(String user, String pw) throws ServletException {
		throw new ServletException("Not implemented");
	}

	@Override
	public void logout() throws ServletException {
		throw new ServletException("Not implemented");
	}

	@Override
	public <T extends HttpUpgradeHandler> T upgrade(Class<T> type) throws IOException, ServletException {
		throw new ServletException("Not implemented");
	}

}
