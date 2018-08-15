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

import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.Rio
import org.eclipse.thingweb.directory.LookUpResult
import org.junit.Test

/**
 * .
 *
 * @author Victor Charpenay
 * @creation 08.08.2018
 *
 */
class RDFSerializerTest {

	@Test
	void testReadWriteContent() {
		def cl = getClass().getClassLoader()
		
		def i = cl.getResourceAsStream('samples/fanTD.jsonld')
		RDFResource res = RDFSerializer.instance.readContent(i, 'application/ld+json')
		
		assert !res.content.isEmpty() : 'RDF resource was not properly deserialized'
		
		def o = new ByteArrayOutputStream()
		RDFSerializer.instance.writeContent(res, o, 'text/turtle')
		
		assert o.toByteArray().length > 0 : 'RDF resource was not properly serialized'
		// TODO more precise testing
	}
	
	@Test
	void testWriteLookUpResult() {
		def cl = getClass().getClassLoader()
		
		def i = cl.getResourceAsStream('samples/fanTD.jsonld')
		def fan = RDFSerializer.instance.readContent(i, 'application/td+json') as RDFResource
		
		i = cl.getResourceAsStream('samples/temperatureSensorTD.jsonld')
		def temp = RDFSerializer.instance.readContent(i, 'application/td+json') as RDFResource
		
		def res = new LookUpResult([fan, temp])
		
		def o = new ByteArrayOutputStream()
		RDFSerializer.instance.writeContent(res, o, 'application/trig')
		
		i = new ByteArrayInputStream(o.toByteArray())
		def g = Rio.parse(i, '', RDFFormat.TRIG)
		
		assert g.contexts().contains(fan.iri) : 'Lookup result was not properly serialized (no identifier for fan TD)'
		assert g.filter(null, null as IRI, null, fan.iri) : 'Lookup result did not include fan TD content'
		assert g.contexts().contains(temp.iri) : 'Lookup result was not properly serialized (no identifier for temperature sensor TD)'
		assert g.filter(null, null as IRI, null, temp.iri) : 'Lookup result did not include temperature sensor TD content'
	}
	
}
