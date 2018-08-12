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

import groovy.json.*

import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.eclipse.rdf4j.model.util.Models
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.Rio
import org.junit.Test

class TDSerializerTest {

	@Test
	void testReadContent() {
		def cl = getClass().getClassLoader()
		
		InputStream i = cl.getResourceAsStream('samples/fanTD.jsonld')
		def res = TDSerializer.instance.readContent(i, TDSerializer.TD_CONTENT_FORMAT) as RDFResource
		
		i = cl.getResourceAsStream('samples/fanTD.ttl')
		def ref = Rio.parse(i, '', RDFFormat.TURTLE, res.iri)

		assert Models.isomorphic(res.graph, ref) : 'TD content was not properly parsed'
	}
	
	@Test
	void testWriteContent() {
		def cl = getClass().getClassLoader()
		
		InputStream i = cl.getResourceAsStream('samples/fanTD.ttl')
		def g = Rio.parse(i, '', RDFFormat.TURTLE)
		
		def res = new RDFResource(g)
		def buf = new ByteArrayOutputStream()
		TDSerializer.instance.writeContent(res, buf, TDSerializer.TD_CONTENT_FORMAT)
		
		i = new ByteArrayInputStream(buf.toByteArray())
		def obj = new JsonSlurper().parse(i)
		
		assert obj.'id' == 'urn:Fan' : 'TD content was not properly serialized (missing mandatory id)'
		// TODO use JSON schema instead
	}
	
}
