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
package org.eclipse.thingweb.directory.vocabulary

import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.impl.SimpleValueFactory

/**
 * Java wrapper of the Thing Description RDF vocabulary
 *
 * @author Victor Charpenay
 * @creation 06.08.2018
 *
 */
class TD {

	public static final String PREFIX = 'td'
	
	public static final String NAMESPACE = 'http://www.w3.org/ns/td#'
	
	public static final IRI THING = SimpleValueFactory.instance.createIRI(NAMESPACE + "Thing")
	
	public static final IRI NAME = SimpleValueFactory.instance.createIRI(NAMESPACE + "name")
	
	private TD() {}
	
}
