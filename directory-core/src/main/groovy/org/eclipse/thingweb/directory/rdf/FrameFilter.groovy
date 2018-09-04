package org.eclipse.thingweb.directory.rdf

import groovy.json.JsonSlurper

import java.util.Map
import java.util.Set

import org.eclipse.thingweb.directory.utils.FrameTransform

/**
 * 
 * Implementation of the {@link org.eclipse.thingweb.directory.LookUpFilter LookUpFilter}
 * interface for search queries expressed as JSON-LD frames, on which frame matching is
 * applied.
 *
 * @see
 *   <a href="https://w3c.github.io/json-ld-framing//">
 *     JSON-LD 1.1 Framing (Editor's Draft)
 *   </a>
 *
 * @author Victor Charpenay
 * @creation 04.09.2018
 *
 */
class FrameFilter extends SPARQLFilter {

	FrameFilter(Map params = [:]) {
		super(params)
	}
	
	@Override
	Set<String> filter(search) {
		def obj = new JsonSlurper().parseText(search)
		String sparqlSearch = new FrameTransform(obj).asSPARQL()
		
		return super.filter(sparqlSearch)
	}
	
}
