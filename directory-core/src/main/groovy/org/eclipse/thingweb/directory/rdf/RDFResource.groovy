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

import java.util.UUID

import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.ModelFactory
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.Statement
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.model.ValueFactory
import org.eclipse.rdf4j.model.impl.LinkedHashModel
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.eclipse.rdf4j.model.util.Models
import org.eclipse.rdf4j.model.vocabulary.DCTERMS
import org.eclipse.rdf4j.query.QueryResults
import org.eclipse.thingweb.directory.Resource as DirectoryResource

/**
 * Implementation of the {@link org.eclipse.thingweb.directory.Resource Resource}
 * interface as an RDF dataset, where the actual content of a resource is stored
 * in an RDF graph.
 * <p>
 * In this implementation, directory resources are interpreted as DCAT datasets.
 * 
 * @see
 *   <a href="https://www.w3.org/TR/vocab-dcat/">
 *     DCAT RDF vocabulary
 *   </a>
 *
 * @author Victor Charpenay
 * @creation 07.08.2018
 *
 */
@Log
class RDFResource implements DirectoryResource {
	
	/**
	 * Resource identifier (randomly generated UUID URN)
	 */
	final Resource iri
	
	/**
	 * Underlying RDF dataset, including content (as an RDF graph) and DCAT meta-data
	 */
	final Model graph
	
	RDFResource(Model g) {
		switch (g.contexts().size()) {
			case { it > 2 }: // default graph, several named graphs
				log.warning('RDF resource has more than one identifier; picking one...')
				
			case 2: // default graph, 1 named graph
				this.iri = g.contexts().find()
				g = g.filter(null, null as IRI, null, iri)
				break
				
			default: // default graph only
				this.iri = idFromGraph(g)
				break
		}
		
		this.graph = new LinkedHashModel()
		
		g.forEach({ Statement st ->
			graph.add(st.subject, st.predicate, st.object, iri)
		})
	}

	String getEp() {
		def st = graph.filter(iri, DCTERMS.PUBLISHER, null)
		return Models.objectIRI(st).orElse(null)
	}
	
	def setEp(String ep) {
		def vf = SimpleValueFactory.instance
		graph.remove(iri, DCTERMS.PUBLISHER, null)
		graph.add(iri, DCTERMS.PUBLISHER, vf.createIRI(ep))
	}
	
	String getLt() {
		// TODO with dct:valid?
	}
	
	def setLt(String lt) {
		// TODO
	}
	
	String getBase() {
		// TODO
	}
	
	def setBase(String base) {
		// TODO
	}
	
	/**
	 * Returns the resource content only (without meta-data)
	 * 
	 * @return an RDF graph representing the resource content
	 */
	Model getContent() {
		def g = new LinkedHashModel()		
		graph.filter(null, null as IRI, null, iri).forEach({ Statement st ->
			g.add(st.subject, st.predicate, st.object)
		})
		
		return g
	}
	
	@Override
	String getId() {
		return iri.stringValue()
	}
	
	@Override
	void merge(DirectoryResource res) {
		if (RDFResource.isInstance(res)) {
			def rdf = res as RDFResource
			graph.addAll(rdf.graph)
		} else {
			log.warn('Trying to merge an RDF resource with a non-RDF resource; nothing done...')
		}
	}
	
	protected IRI idFromGraph(Model g) {
		// TODO normalize graph and always return the same id for the same graph
		return SimpleValueFactory.instance.createIRI('urn:uuid:' + UUID.randomUUID())
	}

}
