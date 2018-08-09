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

import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.Rio
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Literal
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.impl.LinkedHashModel
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.eclipse.rdf4j.model.util.ModelBuilder
import org.eclipse.rdf4j.model.vocabulary.RDF
import org.eclipse.rdf4j.model.vocabulary.RDFS
import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.eclipse.rdf4j.repository.RepositoryResult
import org.eclipse.rdf4j.repository.util.Repositories
import org.junit.Before
import org.junit.Test

/**
 * .
 *
 * @author Victor Charpenay
 * @creation 07.08.2018
 *
 */
class RDFResourceTest {

	@Test
	void testRDFResourceModel() {
		def res = new RDFResource(new LinkedHashModel())
		def regex = '^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?'
		
		assert res.id.matches(regex) : 'RDF document ID is not a URI (as per RFC 3986)'
	}
	
	@Test
	void testEp() {
		def res = new RDFResource(new LinkedHashModel())
		def ep = 'http://example.org'
		res.ep = ep
		
		assert res.ep == ep : 'Attribute "ep" was not updated properly'
	}
	
	@Test
	void testMerge() {
		def b = new ModelBuilder()
		def g1 = b.add('tag:someresource', RDF.TYPE, RDFS.RESOURCE).build()
		def g2 = b.add('tag:someotherresource', RDF.TYPE, RDFS.RESOURCE).build()
		
		def res = new RDFResource(g1)
		def other = new RDFResource(g2)
		res.merge(other)
		
		assert res.graph.contexts().size() == 2 : 'RDF documents are not merged'
	}
	
}
