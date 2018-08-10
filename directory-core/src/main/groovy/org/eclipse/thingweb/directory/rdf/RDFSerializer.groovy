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
package org.eclipse.thingweb.directory.rdf

import java.io.InputStream
import java.io.OutputStream
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.Rio
import org.eclipse.thingweb.directory.Resource
import org.eclipse.thingweb.directory.ResourceSerializer

/**
 * Implementation of the {@link org.eclipse.thingweb.directory.ResourceSerializer ResourceSerializer}
 * interface supporting RDF formats.
 *
 * @author Victor Charpenay
 * @creation 06.08.2018
 *
 */
@Singleton
class RDFSerializer implements ResourceSerializer {

	final static RDFFormat DEFAULT_FORMAT = RDFFormat.JSONLD
	
	final static String DEFAULT_BASE_IRI = 'tag:toremove'
	
	@Override
	Resource readContent(InputStream i, String cf) {
		def format = Rio.getParserFormatForMIMEType(cf).orElse(DEFAULT_FORMAT)
		def g = Rio.parse(i as InputStream, DEFAULT_BASE_IRI, format)
		return new RDFResource(g)
	}

	@Override
	void writeContent(Resource res, OutputStream o, String cf) {
		def format = Rio.getParserFormatForMIMEType(cf).orElse(DEFAULT_FORMAT)
		Rio.write((res as RDFResource).graph, o, format)
	}

}
