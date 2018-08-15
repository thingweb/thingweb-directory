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
import org.eclipse.rdf4j.model.util.ModelBuilder
import org.eclipse.rdf4j.model.util.Models
import org.eclipse.rdf4j.model.vocabulary.DCAT
import org.eclipse.rdf4j.model.vocabulary.DCTERMS
import org.eclipse.rdf4j.model.vocabulary.RDF
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
	
	private static final ValueFactory VALUE_FACTORY = SimpleValueFactory.instance
	
	/**
	 * DCAT meta-data
	 */
	final Model metadata = new LinkedHashModel()
	
	/**
	 * Underlying RDF graph
	 */
	final Model content = new LinkedHashModel()
	
	/**
	 * Resource identifier (defaults to randomly generated UUID URN)
	 */
	Resource iri
	
	RDFResource(Model content, Model metadata = new LinkedHashModel()) {
		if (content.contexts().size() > 1) log.warning('Named graphs in RDF resource content ignored')
		this.content.addAll(content)

		def opt = Models.subjectIRI(metadata.filter(null, RDF.TYPE, DCAT.DATASET))
		if (opt.isPresent()) {			
			this.iri = opt.get()
			
			this.metadata.addAll(metadata.filter(iri, null as IRI, null))
		} else {
			this.iri = generate(content)
			
			this.metadata.add(iri, RDF.TYPE, DCAT.DATASET)
			this.metadata.add(iri, DCTERMS.ISSUED, VALUE_FACTORY.createLiteral(new Date()))
		}
		
		log.fine("Creating RDF resource object with id <${iri}>")
	}

	String getEp() {
		def opt = Models.getPropertyIRI(metadata, iri, DCTERMS.PUBLISHER)
		return opt.isPresent() ? opt.get().stringValue() : null
	}
	
	def setEp(String ep) {
		Models.setProperty(metadata, iri, DCTERMS.PUBLISHER, VALUE_FACTORY.createIRI(ep))
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
	
	@Override
	String getId() {
		return iri.stringValue()
	}
	
	/**
	 * The identifier of an RDF resource is allowed to change over time
	 * 
	 * @param id new identifier
	 */
	void setId(String id) {
		def newIri = resolve(id)
		def newContent = new ModelBuilder().subject(newIri)
		
		def properties = metadata.filter(iri, null as IRI, null)
		properties.forEach({ Statement st ->
			newContent.add(st.predicate, st.object)
		})
		
		properties.clear()
		metadata.addAll(newContent.build())
		
		iri = newIri
	}
	
	void setContent(Model content) {
		this.content.clear()
		this.content.addAll(content)
		
		this.metadata.add(iri, DCTERMS.MODIFIED, VALUE_FACTORY.createLiteral(new Date()))
	}
	
	/**
	 * Generates a default IRI for the given RDF graph
	 * 
	 * @param g an RDF graph
	 * @return a UUID URN
	 */
	static IRI generate(Model g) {
		// TODO normalize graph and always return the same id for a fixed graph
		return SimpleValueFactory.instance.createIRI('urn:uuid:' + UUID.randomUUID())
	}
	
	/**
	 * Resolves the input identifier to an absolute IRI. Relative IRIs are
	 * appended to the default base IRI of the {@link RDFSerializer} class.
	 * 
	 * @param id a resource identifier
	 * @return the identifier as a resolved (absolute) IRI
	 */
	static IRI resolve(String id) {
		try {
			return VALUE_FACTORY.createIRI(id)
		} catch (IllegalArgumentException e) {
			log.info('Provided resource identifier is not an absolute IRI. Resolving with default base IRI...')
			
			// TODO case where id contains invalid chars
			return VALUE_FACTORY.createIRI(RDFSerializer.DEFAULT_BASE_IRI + '/', id)
		}
	}

}
