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

import javax.xml.datatype.DatatypeConstants
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Literal
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.util.Models
import org.eclipse.rdf4j.model.vocabulary.DCTERMS
import org.eclipse.rdf4j.query.QueryResults
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.eclipse.rdf4j.repository.RepositoryResult
import org.eclipse.rdf4j.repository.util.Repositories
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.Rio
import org.eclipse.thingweb.directory.Resource
import org.eclipse.thingweb.directory.vocabulary.TD
import org.junit.Before
import org.junit.Test

/**
 * .
 *
 * @author Victor Charpenay
 * @creation 08.08.2018
 *
 */
class RDFResourceManagerTest {
	
	@Before
	void cleanRepo() {
		def repo = RDFResourceManager.instance.repo
		Repositories.consume(repo, { RepositoryConnection con ->
			String q = 'DROP DEFAULT;'
			
			RepositoryResult r = con.getContextIDs()
			while (r.hasNext()) q += "DROP GRAPH <${r.next()}>;"
			r.close()
			
			con.prepareUpdate(q).execute()
		})
	}

	@Test
	void testRepo() {
		def repo = RDFResourceManager.instance.repo
		
		assert repo.connection.isEmpty() : 'RDF resource manager did not connect to an empty RDF store'
	}
	
	@Test
	void testRegister() {
		def cl = getClass().getClassLoader()
		
		InputStream i = cl.getResourceAsStream('samples/fanTD.jsonld')
		Model g = Rio.parse(i, '', RDFFormat.JSONLD)

		def res = new RDFResource(g)
		RDFResourceManager.instance.register(res)
		
		def repo = RDFResourceManager.instance.repo
		Repositories.consume(repo, { RepositoryConnection con ->
			def r = con.getStatements(res.iri, DCTERMS.ISSUED, null)
			assert r.hasNext() : 'RDF document was not properly inserted in the RDF store (no dct:issued statement)'
			def l = r.next().object as Literal
			assert !r.hasNext() : 'RDF document was not properly inserted in the RDF store (too many dct:issued statements)'
			r.close()
		
			def d = l.calendarValue()
			def now = DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar())
			def c = d.compare(now)
		
			assert c == DatatypeConstants.LESSER || c == DatatypeConstants.EQUAL : 'RDF document insertion time is erroneous'
			
			r = con.getStatements(null, null as IRI, null, res.iri)
			assert r.hasNext() : 'RDF document was not persisted in the RDF store'
			r.close()
		})
	}
	
	@Test
	void testExists() {
		def cl = getClass().getClassLoader()
		
		def exists = RDFResourceManager.instance.exists('tag:someresource')
		
		assert !exists : 'Unregistered resource should not have been recognized by the RDF resource manager'
		
		InputStream i = cl.getResourceAsStream('samples/fanTD.jsonld')
		Model g = Rio.parse(i, '', RDFFormat.JSONLD)

		def res = new RDFResource(g)
		RDFResourceManager.instance.register(res)
		
		exists = RDFResourceManager.instance.exists(res.id)
		
		assert exists : 'Registered resource was not recognized by the RDF resource manager'
	}
	
	@Test
	void testGet() {
		Resource res = RDFResourceManager.instance.get('tag:someresource')
		
		assert res.graph.isEmpty() : 'RDF resource manager returned an inconsistent resource object'
	}
	
	@Test
	void testReplace() {
		def cl = getClass().getClassLoader()
		
		InputStream i = cl.getResourceAsStream('samples/fanTD.jsonld')
		Model g = Rio.parse(i, '', RDFFormat.JSONLD)
		
		def res = new RDFResource(g)
		RDFResourceManager.instance.register(res)
		
		i = cl.getResourceAsStream('samples/fanTD_update.jsonld')
		g = Rio.parse(i, '', RDFFormat.JSONLD)
		
		def other = new RDFResource(g)
		RDFResourceManager.instance.replace(res, other)
		
		def repo = RDFResourceManager.instance.repo
		Repositories.consume(repo, { RepositoryConnection con ->
			def r = con.getStatements(res.iri, DCTERMS.MODIFIED, null)
			assert r.hasNext() : 'RDF document was not properly updated (no dct:modified statement)'
			def l = r.next().object as Literal
			assert !r.hasNext() : 'RDF document was not properly inserted in the RDF store (too many dct:modified statements)'
			r.close()
		
			def d = l.calendarValue()
			def now = DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar())
			def cnow = d.compare(now)
			
			r = con.getStatements(res.iri, DCTERMS.ISSUED, null)
			def issued = (r.next().object as Literal).calendarValue()
			def cissued = d.compare(issued)
			r.close()

			assert cnow == DatatypeConstants.LESSER || cnow == DatatypeConstants.EQUAL : 'RDF document update time is erroneous'
			assert cissued == DatatypeConstants.GREATER || cissued == DatatypeConstants.EQUAL : 'RDF document update time is erroneous'
			
			// FIXME res.iri instead of null (change fixtures?)
			r = con.getStatements(null, TD.NAME, null, res.iri)
			assert r.hasNext() : 'RDF document was not properly updated (expected statement not found)'
			l = r.next().object as Literal
			assert l.stringValue() == 'Fan2' : 'RDF document was not updated'
			r.close()
		})
	}
	
	@Test
	void testDelete() {
		def cl = getClass().getClassLoader()
		
		InputStream i = cl.getResourceAsStream('samples/fanTD.jsonld')
		Model g = Rio.parse(i, '', RDFFormat.JSONLD)
		
		def res = new RDFResource(g)
		RDFResourceManager.instance.register(res)
		RDFResourceManager.instance.delete(res)
		
		res = RDFResourceManager.instance.get(res.id) as RDFResource
		g = res.graph.filter(null, null as IRI, null, res.iri)
		
		assert g.isEmpty() : 'RDF document was not deleted'
	}
	
}
