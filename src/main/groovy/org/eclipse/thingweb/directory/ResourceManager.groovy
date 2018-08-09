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
package org.eclipse.thingweb.directory

import groovy.util.logging.Log
import java.io.InputStream
import java.io.OutputStream
import java.util.List
import java.util.Map

import static Attribute.*

/**
 * .
 *
 * @author Victor Charpenay
 * @creation 06.08.2018
 *
 */
@Log
abstract class ResourceManager {
	
	// TODO interfaces without attrs
	
	static final String DEFAULT_ENDPOINT = 'tag:unknown'
	
	static final Integer DEFAULT_LIFETIME = 9000
	
	static final String DEFAULT_BASE = 'coap://localhost' // TODO use source URI instead
	
	/**
	 * See Section 5.3
	 * 
	 * @param stream
	 * @param cf
	 * @param attrs
	 * @return the resource ID as a String
	 */
	String register(InputStream i, String cf, Map attrs) {
		ResourceSerializer rs = ResourceSerializerFactory.get(cf)
		def res = rs.readContent(i, cf)
		
		// TODO ep is quasi-mandatory
		def ep = attrs[ep] ?: DEFAULT_ENDPOINT
		def lt = attrs[lt] ?: DEFAULT_LIFETIME
		def base = attrs[base] ?: DEFAULT_BASE
		if (attrs[d]) log.warning('Attribute "d" not supported')

		res.ep = ep
		res.lt = lt
		res.base = base
		
		register(res)
		
		return res.id
	}
	
	/**
	 * See Section A.3
	 * 
	 * @param id
	 * @param stream
	 * @param cf
	 * @return
	 */
	void get(String id, OutputStream o, String cf, Map attrs) {
		def res = get(id)
		
		if (attrs.size() > 0) log.warning('No attribute supported')
		
		ResourceSerializer rs = ResourceSerializerFactory.get(cf)
		rs.writeContent(res, o, cf)
	}

	/**
	 * See Section A.1
	 * 
	 * @param id
	 * @param InputStream
	 * @param cf
	 * @param attrs
	 * @return
	 */
	void replace(String id, InputStream i, String cf, Map attrs) {
		ResourceSerializer rs = ResourceSerializerFactory.get(cf)
		def res = rs.readContent(i, cf)
		
		def lt = attrs[lt] ?: (res.lt ? res.lt : DEFAULT_LIFETIME)
		def base = attrs[base] ?: (res.base ? res.base : DEFAULT_BASE)
		
		res.lt = lt
		res.base = base
		
		replace(get(id), res)
	}
	
	/**
	 * See Section A.2
	 * 
	 * @param id
	 * @return
	 */
	void delete(String id) {
		delete(get(id))
	}
	
	/**
	 * See Section 7.2
	 * 
	 * @return
	 */
	void lookUp(String type, OutputStream o, String cf, Map attrs, search) {
		LookUpFilter f = LookUpFilterFactory.get(type, this)
		def ids = f.filter(search).asList()
		
		def page = attrs[page] ?: 0
		def count = attrs[count] ?: ids.size()
		
		if (page && !count) log.warning('Attribute "page" used without attribute "count"')
		
		// TODO paging on links not on resources
		ids = ids.subList(page * count, (page + 1) * count)
		
		if (!ids.isEmpty()) {
			def res = ids.collect({ id -> get(id) }).inject({ all, res -> all.merge(res) })
			
			ResourceSerializer rs = ResourceSerializerFactory.get(cf)
			rs.writeContent(res, o, cf)
		}
	}
	
	abstract protected void register(Resource res)
	
	abstract protected Resource get(String id)
	
	abstract protected void replace(Resource res, Resource other)
	
	abstract protected void delete(Resource res)
	
}
