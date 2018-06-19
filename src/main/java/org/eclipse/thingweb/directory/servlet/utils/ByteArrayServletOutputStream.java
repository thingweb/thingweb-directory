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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
	
public class ByteArrayServletOutputStream extends ServletOutputStream {

	ByteArrayOutputStream out = new ByteArrayOutputStream();
	
	public byte[] getBytes() {
		return out.toByteArray();
	}
	
	@Override
	public boolean isReady() {
		return true;
	}

	@Override
	public void setWriteListener(WriteListener arg0) {
		// TODO ?
	}

	@Override
	public void write(int b) throws IOException {
		out.write(b);
	}
	
	public void reset() {
		out.reset();
	}
	
	public int size() {
		return out.size();
	}
	
	@Override
	public void flush() throws IOException {
		out.flush();
	}
	
}