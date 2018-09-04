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
package org.eclipse.thingweb.directory.utils

import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.Rio
import groovy.json.*

/**
 * 
 * Transformation script from JSON-LD frame to SPARQL query.
 * 
 * Notes:
 * <ul>
 *   <li>frame annotations ({@code @embed}, {@code @default}...) are not supported,</li>
 *   <li>frame with JSON-LD 1.1 are not supported.</li>
 * </ul>
 *
 * @author Victor Charpenay
 * @author Anh Le-Tuan
 * @creation 03.09.2018
 *
 */
class FrameTransform {

	final Model frame
	
	FrameTransform(input) {
		// TODO instead, expand JSON-LD input (with annotations) and produce RDF graph manually
		InputStream i = new ByteArrayInputStream(JsonOutput.toJson(input).bytes)
		frame = Rio.parse(i, '', RDFFormat.JSONLD)
	}
	
	def asSPARQL() {
		ByteArrayOutputStream o = new ByteArrayOutputStream()
		Rio.write(frame, o, RDFFormat.NTRIPLES)
		
		return o.toString().replace('_:', '?')
	}
	
}
