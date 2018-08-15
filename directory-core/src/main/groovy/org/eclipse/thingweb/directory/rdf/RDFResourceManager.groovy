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
import org.eclipse.rdf4j.query.QueryResults
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
	
	private final String registrationType
	
	private final String preferredContentFormat
	
	final Repository repo
	
	RDFResourceManager(String rd, String cf, Map params = [:]) {
		registrationType = rd
		preferredContentFormat = cf
		repo = RepositoryFactory.get(params)
		factoryParameters.putAll(params)
	}
	
	@Override
	public String getRegistrationType() {
		return registrationType
	}
	
	@Override
	public String getPreferredContentFormat() {
		return preferredContentFormat
	}
	
	@Override
	protected void register(Resource res) {
		RDFResource rdf
		
		if (RDFResource.isInstance(res)) {
			rdf = res as RDFResource
		} else {
			log.warning('Trying to register a non-RDF resource; content not read...')
			
			rdf = new RDFResource(new LinkedHashModel())
		}

		Repositories.consume(repo, { RepositoryConnection con ->
			con.add(rdf.metadata)
			con.add(rdf.content, rdf.iri)
		})
	}
	
	@Override
	protected boolean exists(String id) {
		def iri = RDFResource.resolve(id)
		
		return Repositories.get(repo, { RepositoryConnection con ->
			def q = "ASK WHERE { <${iri}> a <${DCAT.DATASET}> }"
			return con.prepareBooleanQuery(q).evaluate()
		})
	}
	
	@Override
	protected Resource get(String id) {
		def iri = RDFResource.resolve(id)
		
		RDFResource res = Repositories.get(repo, { RepositoryConnection con ->		
			def q = "CONSTRUCT { ?s ?p ?o } WHERE { GRAPH <${iri}> { ?s ?p ?o }}"
			def r = con.prepareGraphQuery(q).evaluate()
			Model content = QueryResults.asModel(r)
			
			q = "CONSTRUCT { <${iri}> ?p ?o } WHERE { <${iri}> ?p ?o . FILTER NOT EXISTS { GRAPH <${iri}> { ?s ?p ?o }}}"
			r = con.prepareGraphQuery(q).evaluate()
			Model metadata = QueryResults.asModel(r)
			
			return new RDFResource(content, metadata) 
		})
		
		// TODO if non-existing, then id different. Return null instead?
		if (res.id != id) res.id = id
		
		return res
	}
	
	@Override
	protected void replace(Resource res, Resource other) {
		RDFResource rdf, otherRdf

		if (RDFResource.isInstance(res)) {
			rdf = res as RDFResource
		} else {
			rdf = get(res.id) as RDFResource
		}
		
		if (RDFResource.isInstance(other)) {
			rdf.content = (other as RDFResource).content
		} else {
			log.warning('Trying to replace RDF resource with a non-RDF resource; content not read...')
		}
		
		Repositories.consume(repo, { RepositoryConnection con ->
			con.remove(rdf.iri, null as IRI, null)
			con.add(rdf.metadata)
			
			con.clear(rdf.iri)
			con.add(rdf.content, rdf.iri)
		})
	}
	
	@Override
	protected void delete(Resource res) {
		def iri = RDFResource.isInstance(res) ? (res as RDFResource).iri : RDFResource.resolve(res.id)
		
		Repositories.consume(repo, { RepositoryConnection con ->
			con.clear(iri)
		})
	}

}
