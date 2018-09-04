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
import org.eclipse.rdf4j.model.Statement
import org.eclipse.rdf4j.model.util.Models
import org.eclipse.rdf4j.model.vocabulary.DCTERMS
import org.eclipse.rdf4j.query.QueryResults
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.eclipse.rdf4j.repository.RepositoryResult
import org.eclipse.rdf4j.repository.util.Repositories
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.Rio
import org.eclipse.thingweb.directory.Resource
import org.eclipse.thingweb.directory.ResourceManagerFactory
import org.eclipse.thingweb.directory.vocabulary.TD
import org.junit.After
import org.junit.Test

/**
 * .
 *
 * @author Victor Charpenay
 * @creation 08.08.2018
 *
 */
class RDFResourceManagerTest {
	
	final RDFResourceManager m = ResourceManagerFactory.get('vocab')
	
	@After
	void cleanRepo() {
		def repo = m.repo
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
		def repo = m.repo
		
		assert repo.connection.isEmpty() : 'RDF resource manager did not connect to an empty RDF store'
	}
	
	@Test
	void testRegister() {
		def cl = getClass().getClassLoader()
		
		InputStream i = cl.getResourceAsStream('samples/fanTD.ttl')
		Model g = Rio.parse(i, '', RDFFormat.TURTLE)

		def res = new RDFResource(g)
		m.register(res)
		
		def repo = m.repo
		Repositories.consume(repo, { RepositoryConnection con ->
			List<Statement> results = QueryResults.asList(con.getStatements(res.iri, DCTERMS.ISSUED, null))
			assert results.size() == 1 : 'RDF document was not properly inserted in the RDF store (dct:issued statement expected)'
			def l = results[0].object as Literal
		
			def d = l.calendarValue()
			def now = DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar())
			def c = d.compare(now)
		
			assert c == DatatypeConstants.LESSER || c == DatatypeConstants.EQUAL : 'RDF document insertion time is erroneous'
			
			results = QueryResults.asList(con.getStatements(null, null as IRI, null, res.iri))
			assert results.size() > 0 : 'RDF document was not persisted in the RDF store'
		})
	}
	
	@Test
	void testExists() {
		def cl = getClass().getClassLoader()
		
		def exists = m.exists('tag:someresource')
		
		assert !exists : 'Unregistered resource should not have been recognized by the RDF resource manager'
		
		InputStream i = cl.getResourceAsStream('samples/fanTD.ttl')
		Model g = Rio.parse(i, '', RDFFormat.TURTLE)

		def res = new RDFResource(g)
		m.register(res)
		
		exists = m.exists(res.id)
		
		assert exists : 'Registered resource was not recognized by the RDF resource manager'
	}
	
	@Test
	void testGet() {
		def cl = getClass().getClassLoader()
		
		RDFResource res = m.get('tag:someresource') as RDFResource
		
		assert res.content.empty : 'RDF resource manager returned an inconsistent resource object'
		
		InputStream i = cl.getResourceAsStream('samples/fanTD.ttl')
		Model g = Rio.parse(i, '', RDFFormat.TURTLE)
		
		m.repo.connection.add(g, res.iri)
		res = m.get('tag:someresource') as RDFResource
		
		assert !res.content.empty : 'RDF resource manager returned an inconsistent resource object'
	}
	
	@Test
	void testReplace() {
		def cl = getClass().getClassLoader()
		
		InputStream i = cl.getResourceAsStream('samples/fanTD.ttl')
		Model g = Rio.parse(i, '', RDFFormat.TURTLE)
		
		def res = new RDFResource(g)
		m.register(res)
		
		i = cl.getResourceAsStream('samples/fanTD_update.ttl')
		g = Rio.parse(i, '', RDFFormat.TURTLE)
		
		def other = new RDFResource(g)
		m.replace(res, other)
		
		def repo = m.repo
		Repositories.consume(repo, { RepositoryConnection con ->
			List<Statement> results = QueryResults.asList(con.getStatements(res.iri, DCTERMS.MODIFIED, null))
			assert results.size() == 1 : 'RDF document was not properly updated (dct:modified statement expected)'
			def l = results[0].object as Literal
		
			def d = l.calendarValue()
			def now = DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar())
			def cnow = d.compare(now)
			
			results = QueryResults.asList(con.getStatements(res.iri, DCTERMS.ISSUED, null))
			def issued = (results[0].object as Literal).calendarValue()
			def cissued = d.compare(issued)

			assert cnow == DatatypeConstants.LESSER || cnow == DatatypeConstants.EQUAL : 'RDF document update time is erroneous'
			assert cissued == DatatypeConstants.GREATER || cissued == DatatypeConstants.EQUAL : 'RDF document update time is erroneous'
			
			def id = con.getValueFactory().createIRI('urn:Fan')
			results = QueryResults.asList(con.getStatements(id, TD.NAME, null, res.iri))
			assert results.size() == 1 : 'RDF document was not properly updated (td:name statement expected)'
			l = results[0].object as Literal
			assert l.stringValue() == 'Fan2' : 'RDF document was not updated'
		})
	}
	
	@Test
	void testDelete() {
		def cl = getClass().getClassLoader()
		
		InputStream i = cl.getResourceAsStream('samples/fanTD.ttl')
		Model g = Rio.parse(i, '', RDFFormat.TURTLE)
		
		def res = new RDFResource(g)
		m.register(res)
		m.delete(res)
		
		res = m.get(res.id) as RDFResource
		g = res.content
		
		assert g.isEmpty() : 'RDF document was not deleted'
	}
	
}
