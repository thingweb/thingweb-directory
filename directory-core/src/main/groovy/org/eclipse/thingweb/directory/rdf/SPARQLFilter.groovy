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

import org.eclipse.rdf4j.query.QueryResults
import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.repository.util.Repositories
import org.eclipse.thingweb.directory.LookUpFilter

/**
 * Implementation of the {@link org.eclipse.thingweb.directory.LookUpFilter LookUpFilter}
 * interface for search queries expressed in the SPARQL query language.
 *
 * @see
 *   <a href="http://www.w3.org/TR/sparql11-query/">
 *     SPARQL 1.1 Query Language
 *   </a>
 *
 * @author Victor Charpenay
 * @creation 06.08.2018
 *
 */
class SPARQLFilter implements LookUpFilter {
	
	/**
	 * RDF repository against which filter queries are run
	 */
	Repository repo

	/**
	 * Filters resources that match the given SPARQL graph pattern
	 * 
	 * @param search a SPARQL graph pattern, as plain string
	 */
	@Override
	Set<String> filter(search) {
		def q = "SELECT DISTINCT ?id WHERE { GRAPH ?id { ${search} }}"
		def ids = Repositories.tupleQuery(repo, q, { r ->
			def iris = QueryResults.asSet(r)
			return iris.collect({ b -> b.getValue('id').stringValue() })
		})
		return ids;
	}
	
}
