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
package org.eclipse.thingweb.directory.servlet.utils;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * 
 * Wrapper that keeps the response in a byte buffer for later use,
 * instead of directly sending it to the client.
 *
 * @author Victor Charpenay
 * @creation 07.02.2018
 *
 */
public class BufferedResponseWrapper extends HttpServletResponseWrapper {
	
	ByteArrayServletOutputStream out = new ByteArrayServletOutputStream();

	public BufferedResponseWrapper(HttpServletResponse response) {
		super(response);
	}
	
	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		return out;
	}
	
	@Override
	public int getBufferSize() {
		return out.size();
	}
	
	@Override
	public PrintWriter getWriter() throws IOException {
		return new PrintWriter(out);
	}
	
	public byte[] getOutputBytes() {
		return out.getBytes();
	}
	
	@Override
	public void sendError(int sc) throws IOException {
		setStatus(sc);
		// do nothing. Response should not be committed
	}
	
	@Override
	public void sendError(int sc, String msg) throws IOException {
		sendError(sc);
	}
	
	@Override
	public void sendRedirect(String location) throws IOException {
		setStatus(SC_TEMPORARY_REDIRECT);
		// do nothing. Response should not be committed
	}

}
