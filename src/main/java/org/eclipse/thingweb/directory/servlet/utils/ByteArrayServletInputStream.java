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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
	
public class ByteArrayServletInputStream extends ServletInputStream {

	private final ByteArrayInputStream in;
	
	public ByteArrayServletInputStream(byte[] bytes) {
		 in = new ByteArrayInputStream(bytes);
	}
	
	@Override
	public boolean isReady() {
		return true;
	}
	
	@Override
	public boolean isFinished() {
		return true;
	}
	
	@Override
	public int read() throws IOException {
		return in.read();
	}
	
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return in.read(b, off, len);
	}
	
	@Override
	public int read(byte[] b) throws IOException {
		return in.read(b);
	}
	
	@Override
	public void setReadListener(ReadListener arg0) {
		// TODO ?
	}
	
}