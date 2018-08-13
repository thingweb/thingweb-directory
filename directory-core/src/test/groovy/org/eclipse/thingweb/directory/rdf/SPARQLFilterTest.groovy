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

import org.junit.Before
import org.junit.Test

import org.eclipse.thingweb.directory.ResourceManagerFactory

/**
 * .
 *
 * @author Victor Charpenay
 * @creation 09.08.2018
 *
 */
class SPARQLFilterTest {
	
	final RDFResourceManager m = ResourceManagerFactory.get('vocab')

	@Test
	void testFilter() {
		def cl = getClass().getClassLoader()
		
		def i = cl.getResourceAsStream('samples/fanTD.jsonld')
		def fan = m.register(i, 'application/ld+json', [:])
		
		i = cl.getResourceAsStream('samples/temperatureSensorTD.jsonld')
		def temp = m.register(i, 'application/ld+json', [:])
		
		def f = new SPARQLFilter()
		f.repo = m.repo
		
		Set<String> filtered = f.filter('?s ?p ?o')
		
		assert filtered.size() == 2 : 'SPARQL filter did not keep all resources'
		assert filtered.contains(fan) : 'SPARQL filter did not keep fan resource'
		assert filtered.contains(temp) : 'SPARQL filter did not keep temperature resource'
		
		// TODO fix JSON-LD context first
//		filtered = f.filter('?thing a <http://uri.etsi.org/m2m/saref#Sensor> .\n'
//			+ 'FILTER NOT EXISTS {'
//			+ '  ?thing <http://www.w3.org/ns/td#actions> ?i .\n'
//			+ '  ?i a <http://uri.etsi.org/m2m/saref#ToggleCommand> .\n'
//			+ '}')
		
		filtered = f.filter('?thing <http://www.w3.org/ns/td#name> "myTempSensor"')
		
		assert filtered.size() == 1 : 'SPARQL filter did not filter out fan resource'
		assert filtered.contains(temp) : 'SPARQL filter did not keep temperature resource'
	}
	
}
