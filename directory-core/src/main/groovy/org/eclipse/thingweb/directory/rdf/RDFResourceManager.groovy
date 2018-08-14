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

import groovy.util.logging.Log

import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.Statement
import org.eclipse.rdf4j.model.ValueFactory
import org.eclipse.rdf4j.model.impl.LinkedHashModel
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.eclipse.rdf4j.model.vocabulary.DC
import org.eclipse.rdf4j.model.vocabulary.DCTERMS
import org.eclipse.rdf4j.model.vocabulary.DCAT
import org.eclipse.rdf4j.model.vocabulary.RDF
import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.eclipse.rdf4j.repository.RepositoryException
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository
import org.eclipse.rdf4j.repository.util.Repositories
import org.eclipse.rdf4j.sail.memory.MemoryStore
import org.eclipse.thingweb.directory.Resource
import org.eclipse.thingweb.directory.ResourceManager

/**
 * Implementation of the {@link org.eclipse.thingweb.directory.ResourceManager ResourceManager}
 * class, backed by an RDF store. If no SPARQL endpoint is provided, a transient in-memory
 * store is created. Connection to a remote SPARQL endpoint can be configured with the following
 * parameters:
 * <ul>
 *   <li>{@link org.eclipse.rdf4j.repository.rdf.sparqlQueryEndpoint}</li>
 *   <li>{@link org.eclipse.rdf4j.repository.rdf.sparqlUpdateEndpoint} (optional, defaults to query endpoint)</li>
 *   <li>{@link org.eclipse.rdf4j.repository.rdf.sparqlUsername} (optional)</li>
 *   <li>{@link org.eclipse.rdf4j.repository.rdf.sparqlPassword} (optional)</li>
 * </ul>
 *
 * @author Victor Charpenay
 * @creation 06.08.2018
 *
 */
@Log
class RDFResourceManager extends ResourceManager {
	
	private final ValueFactory vf = SimpleValueFactory.instance
	
	private final String preferredContentFormat
	
	final Repository repo
	
	RDFResourceManager(String cf) {
		this(cf, [:])
	}
	
	RDFResourceManager(String cf, Map params) {
		preferredContentFormat = cf
		repo = RepositoryFactory.get(params)
	}
	
	@Override
	public String getPreferredContentFormat() {
		return preferredContentFormat
	}
	
	@Override
	protected void register(Resource res) {
		def iri = resolve(res.id)
		def g = new LinkedHashModel()
		
		if (RDFResource.isInstance(res)) {
			g = (res as RDFResource).graph
		} else {
			log.warning('Trying to register a non-RDF resource; content not read...')
		}

		Repositories.consume(repo, { RepositoryConnection con ->
			con.add(iri, RDF.TYPE, DCAT.DATASET)
			con.add(iri, DCTERMS.ISSUED, vf.createLiteral(new Date()))
			con.add(g)
		})
	}
	
	@Override
	protected boolean exists(String id) {
		def iri = resolve(id)
		
		return Repositories.get(repo, { RepositoryConnection con ->
			def q = "ASK WHERE { <${iri}> a <${DCAT.DATASET}> }"
			return con.prepareBooleanQuery(q).evaluate()
		})
	}
	
	@Override
	protected Resource get(String id) {
		def iri = resolve(id)
		
		def g = Repositories.get(repo, { RepositoryConnection con ->
			def g = new LinkedHashModel()
			
			// content
			def q = "CONSTRUCT { ?s ?p ?o } WHERE { GRAPH <${iri}> { ?s ?p ?o }}"
			def r = con.prepareGraphQuery(q).evaluate()
			while (r.hasNext()) {
				Statement st = r.next()
				g.add(st.subject, st.predicate, st.object, iri)
			}
			
			if (!g.isEmpty()) {
				// meta-data
				q = "CONSTRUCT { <${iri}> ?p ?o } WHERE { <${iri}> ?p ?o . FILTER NOT EXISTS { GRAPH <${iri}> { ?s ?p ?o }}}"
				r = con.prepareGraphQuery(q).evaluate()
				while (r.hasNext()) {
					Statement st = r.next()
					g.add(st.subject, st.predicate, st.object)
				}
			}
			
			return g
		})
		
		return new RDFResource(g);
	}
	
	@Override
	protected void replace(Resource res, Resource other) {
		def iri = resolve(res.id)
		def g = new LinkedHashModel()

		if (RDFResource.isInstance(other)) {
			def rdf = other as RDFResource
			g = rdf.graph.filter(null, null as IRI, null, rdf.iri)
		} else {
			log.warning('Trying to replace RDF resource by a non-RDF resource; content not read...')
		}
		
		// TODO check base, lt
		Repositories.consume(repo, { RepositoryConnection con ->
			con.add(iri, DCTERMS.MODIFIED, vf.createLiteral(new Date()))
			con.clear(iri)
			con.add(g, iri)
		})
	}
	
	@Override
	protected void delete(Resource res) {
		def iri = resolve(res.id)
		
		Repositories.consume(repo, { RepositoryConnection con ->
			con.clear(iri)
		})
	}
	
	/**
	 * Resolves the input identifier to an absolute IRI. Relative IRIs are
	 * appended to the default base IRI of the {@link RDFSerializer} class.
	 * 
	 * @param id a resource identifier
	 * @return the identifier as a resolved (absolute) IRI
	 */
	private IRI resolve(String id) {
		try {
			return vf.createIRI(id)
		} catch (IllegalArgumentException e) {
			// TODO case where id contains invalid chars
			log.info('Provided resource identifier is not an absolute IRI. Resolving with default base IRI...')
			return vf.createIRI(RDFSerializer.DEFAULT_BASE_IRI + '/', id)
		}
	}

}
