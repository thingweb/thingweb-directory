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
package org.eclipse.thingweb.directory.rest;

/**
 * 
 * A Servlet container can add REST resources and collections.
 *
 * @author Victor Charpenay
 * @creation 06.02.2018
 *
 */
public abstract class RESTServletContainer {
	
	public void addServletWithMapping(String path, RESTServlet servlet) {
		if (servlet instanceof CollectionServlet) {
			addServletWithMapping(path + "/*", ((CollectionServlet) servlet).getItemServlet());
		}
	}
	
	public abstract void start();
	
	public abstract void stop();
	
	public abstract void join();
  
}
