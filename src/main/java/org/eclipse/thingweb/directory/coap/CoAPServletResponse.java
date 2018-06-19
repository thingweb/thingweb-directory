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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.coap.CoAP.Code;
import org.eclipse.thingweb.directory.ThingDirectory;
import org.eclipse.thingweb.directory.servlet.utils.ByteArrayServletOutputStream;

public class CoAPServletResponse implements HttpServletResponse {
	
	private String status;
	
	private Map<String, Set<String>> headers = new HashMap<>();
	
	private ByteArrayServletOutputStream out = new ByteArrayServletOutputStream();
	
	public Response asResponse() {
		ResponseCode code = ResponseCode.valueOf(status);
		Response resp = new Response(code);
		
		resp.setPayload(out.getBytes());
		
		return resp;
	}

	@Override
	public void flushBuffer() throws IOException {
		try {
			out.flush();
		} catch (IOException e) {
			ThingDirectory.LOG.warn("Could not flush CoAP buffer", e);
		}
	}

	@Override
	public int getBufferSize() {
		return out.size();
	}

	@Override
	public String getCharacterEncoding() {
		return headers.get("Content-Encoding").iterator().next();
	}

	@Override
	public String getContentType() {
		return headers.get("Content-Type").iterator().next();
	}

	@Override
	public Locale getLocale() {
		return Locale.GERMANY;
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		return out;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		return new PrintWriter(out);
	}

	@Override
	public boolean isCommitted() {
		return false;
	}

	@Override
	public void reset() {
		resetBuffer();
		headers = new HashMap<>();
		status = null;
	}

	@Override
	public void resetBuffer() {
		out.reset();
	}

	@Override
	public void setBufferSize(int size) {
		// do nothing
	}

	@Override
	public void setCharacterEncoding(String encoding) {
		addHeader("Content-Encoding", encoding);
	}

	@Override
	public void setContentLength(int len) {
		addIntHeader("Content-Length", len);
	}

	@Override
	public void setContentLengthLong(long len) {
		addHeader("Content-Length", Long.toString(len));
	}

	@Override
	public void setContentType(String ct) {
		addHeader("Content-Type", ct);
	}

	@Override
	public void setLocale(Locale locale) {
		// do nothing
	}

	@Override
	public void addCookie(Cookie cookie) {
		// do nothing
	}

	@Override
	public void addDateHeader(String name, long value) {
		addHeader(name, Long.toString(value));
	}

	@Override
	public void addHeader(String name, String value) {
		if (!headers.containsKey(name)) {
			setHeader(name, value);
		} else {
			headers.get(name).add(value);
		}
	}

	@Override
	public void addIntHeader(String name, int value) {
		addHeader(name, Integer.toString(value));
	}

	@Override
	public boolean containsHeader(String name) {
		return headers.containsKey(name);
	}

	@Override
	public String encodeRedirectURL(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String encodeRedirectUrl(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String encodeURL(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String encodeUrl(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getHeader(String name) {
		return headers.get(name).iterator().next();
	}

	@Override
	public Collection<String> getHeaderNames() {
		return headers.keySet();
	}

	@Override
	public Collection<String> getHeaders(String name) {
		return headers.get(name);
	}

	@Override
	public int getStatus() {
		return Integer.parseInt(status);
	}

	@Override
	public void sendError(int code) throws IOException {
		setStatus(code);
	}

	@Override
	public void sendError(int code, String message) throws IOException {
		setStatus(code);
		getOutputStream().write(message.getBytes());
	}

	@Override
	public void sendRedirect(String location) throws IOException {
		setStatus(307); // note: not in RFC7252
		setHeader("Location", location);
	}

	@Override
	public void setDateHeader(String name, long value) {
		setHeader(name, Long.toString(value));
	}

	@Override
	public void setHeader(String name, String value) {
		Set<String> set = new HashSet<>();
		set.add(value);
		headers.put(name, set);
	}

	@Override
	public void setIntHeader(String name, int value) {
		setHeader(name, Integer.toString(value));
	}

	@Override
	public void setStatus(int code) {
		status = Integer.toString(code);
	}

	@Override
	public void setStatus(int code, String message) {
		setStatus(code);
	}

}
