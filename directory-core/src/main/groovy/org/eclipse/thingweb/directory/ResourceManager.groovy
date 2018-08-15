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
 * Programmatic interface aligned with the IETF CoRE Resource Directory Draft.
 * This class includes generic processing of e.g. request attributes and delegates
 * persistence-specific CRUD operations to sub-classes.
 *
 * @see
 *   <a href="https://tools.ietf.org/html/draft-ietf-core-resource-directory-14">
 *     CoRE Resource Directory Draft
 *   </a>
 *   
 * @see Resource
 * @see ResourceSerializer
 * @see LookUpFilter
 *
 * @author Victor Charpenay
 * @creation 06.08.2018
 *
 */
@Log
abstract class ResourceManager {
	
	/**
	 * Default value if no {@code ep} attribute is provided in the request.
	 */
	static final String DEFAULT_ENDPOINT = 'tag:unknown'
	
	/**
	 * Default value for the {@code lt} attribute (25h).
	 */
	static final Integer DEFAULT_LIFETIME = 9000
	
	/**
	 * Default value if no {@code base} attribute is provided in the request.
	 */
	static final String DEFAULT_BASE = 'coap://localhost' // TODO use source URI instead
	
	/**
	 * Preferred resource content format to be process by this resource manager.
	 * E.g. {@code application/link-format}.
	 * 
	 * @returns the manager object's preferred content format
	 */
	abstract String getPreferredContentFormat()
	
	/**
	 * Equivalent to {@code ResourceManager.resgister(i, cf, [:])} (empty attribute map).
	 * 
	 * @param i streamed resource content
	 * @param cf resource content format, IANA-registered media type (e.g. {@code application/link-format})
	 * @return resource identifier, for later reference (also called {@code location})
	 * @throws ResourceAlreadyRegisteredException if a resource with the same identifier already exists
	 */
	String register(InputStream i, String cf) throws ResourceAlreadyRegisteredException {
		return register(i, cf, [:])
	}
	
	/**
	 * Registration request interface.
	 * <p>
	 * Endpoint: {@code {+rd}{?ep,d,lt,base,extra-attrs*}}
	 * 
	 * @see
	 *   <a href="https://tools.ietf.org/html/draft-ietf-core-resource-directory-14#section-5.3">
	 *     CoRE Resource Directory Draft (Section 5.3)
	 *   </a>
	 * 
	 * @param i streamed resource content
	 * @param cf resource content format, IANA-registered media type (e.g. {@code application/link-format})
	 * @param attrs request attributes (e.g. {@code ep}, {@code d}, {@code lt}, {@code base})
	 * @return resource identifier, for later reference (also called {@code location})
	 * @throws ResourceAlreadyRegisteredException if a resource with the same identifier already exists
	 */
	String register(InputStream i, String cf, Map attrs) throws ResourceAlreadyRegisteredException {
		ResourceSerializer rs = ResourceSerializerFactory.get(cf)
		def res = rs.readContent(i, cf)
		
		if (exists(res.id)) throw new ResourceAlreadyRegisteredException(res.id)
		
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
	 * Equivalent to {@code ResourceManager.get(id, o, cf, [:])} (empty attribute map).
	 * 
	 * @param id resource identifier (aka {@code location})
	 * @param o stream to which resource content will be written (in the format specified by {@code cf})
	 * @param cf resource content format, IANA-registered media type (e.g. {@code application/link-format})
	 * @throws ResourceNotRegisteredException if the resource identifier is unknown
	 */
	void get(String, id, OutputStream o, String cf) throws ResourceNotRegisteredException {
		get(id, o, cf, [:])
	}
	
	/**
	 * Read request interface.
	 * <p>
	 * Endpoint: {@code {+location}{?href,rel,rt,if,ct}}
	 * 
	 * @see
	 *   <a href="https://tools.ietf.org/html/draft-ietf-core-resource-directory-14#appendix-A.3">
	 *     CoRE Resource Directory Draft (Section A.3)
	 *   </a>
	 * 
	 * @param id resource identifier (aka {@code location})
	 * @param o stream to which resource content will be written (in the format specified by {@code cf})
	 * @param cf resource content format, IANA-registered media type (e.g. {@code application/link-format})
	 * @param attrs request attributes ({@code href}, {@code rel}, {@code rt}, {@code if}, {@code ct})
	 * @throws ResourceNotRegisteredException if the resource identifier is unknown
	 */
	void get(String id, OutputStream o, String cf, Map attrs) throws ResourceNotRegisteredException {
		if (!exists(id)) throw new ResourceNotRegisteredException()
		
		def res = get(id)
		
		if (attrs.size() > 0) log.warning('No attribute supported')
		
		ResourceSerializer rs = ResourceSerializerFactory.get(cf)
		rs.writeContent(res, o, cf)
	}
	
	/**
	 * Equivalent to {@code ResourceManager.replace(id, i, cf, [:])} (empty attribute map).
	 *
	 * @param id id resource identifier (aka {@code location})
	 * @param i streamed resource content to substitute to current content
	 * @param cf resource content format, IANA-registered media type (e.g. {@code application/link-format})
	 * @throws ResourceNotRegisteredException if the resource identifier is unknown
	 */
	void replace(String id, InputStream i, String cf) throws ResourceNotRegisteredException {
		replace(id, i, cf, [:])
	}

	/**
	 * Update registration request interface.
	 * <p>
	 * Endpoint: {@code {+location}{?lt,con,extra-attrs*}}
	 * 
	 * @see
	 *   <a href="https://tools.ietf.org/html/draft-ietf-core-resource-directory-14#appendix-A.1">
	 *     CoRE Resource Directory Draft (Section A.1)
	 *   </a>
	 * 
	 * @param id id resource identifier (aka {@code location})
	 * @param i streamed resource content to substitute to current content
	 * @param cf resource content format, IANA-registered media type (e.g. {@code application/link-format})
	 * @param attrs request attributes (e.g. {@code lt}, {@code con})
	 * @throws ResourceNotRegisteredException if the resource identifier is unknown
	 */
	void replace(String id, InputStream i, String cf, Map attrs) throws ResourceNotRegisteredException {
		if (!exists(id)) throw new ResourceNotRegisteredException()

		ResourceSerializer rs = ResourceSerializerFactory.get(cf)
		def res = rs.readContent(i, cf)
		
		def lt = attrs[lt] ?: (res.lt ? res.lt : DEFAULT_LIFETIME)
		def base = attrs[base] ?: (res.base ? res.base : DEFAULT_BASE)
		
		res.lt = lt
		res.base = base
		
		replace(get(id), res)
	}
	
	/**
	 * 
	 * Removal request interface.
	 * <p>
	 * Endpoint: {@code {+location}}
	 * 
	 * @see
	 *   <a href="https://tools.ietf.org/html/draft-ietf-core-resource-directory-14#appendix-A.2">
	 *     CoRE Resource Directory Draft (Section A.2)
	 *   </a>
	 * 
	 * @param id id resource identifier (aka {@code location})
	 * @throws ResourceNotRegisteredException if the resource identifier is unknown
	 */
	void delete(String id) throws ResourceNotRegisteredException {
		if (!exists(id)) throw new ResourceNotRegisteredException()

		delete(get(id))
	}
	
	/**
	 * Equivalent to {@code ResourceManager.lookUp(type, o, cf, search, [:])} (empty attribute map).
	 *
	 * @param type type of lookup (e.g. {@code ep}, {@code res})
	 * @param o stream to which resource content will be written (in the format specified by {@code cf})
	 * @param cf resource content format, IANA-registered media type (e.g. {@code application/link-format})
	 * @param search search query (depending on the type of lookup)
	 */
	void lookUp(String type, OutputStream o, String cf, search) {
		
	}
	
	/**
	 * Lookup interface.
	 * <p>
	 * Endpoint: {@code {+type-lookup-location}{?page,count,search*}}
	 * 
	 * @see
	 *   <a href="https://tools.ietf.org/html/draft-ietf-core-resource-directory-14#section-7.2">
	 *     CoRE Resource Directory Draft (Section 7.2)
	 *   </a>
	 * 
	 * @param type type of lookup (e.g. {@code ep}, {@code res})
	 * @param o stream to which resource content will be written (in the format specified by {@code cf})
	 * @param cf resource content format, IANA-registered media type (e.g. {@code application/link-format})
	 * @param search search query (depending on the type of lookup)
	 * @param attrs request attributes ({@code page}, {@code count})
	 */
	void lookUp(String type, OutputStream o, String cf, search, Map attrs) {
		LookUpFilter f = LookUpFilterFactory.get(type, this)
		def ids = f.filter(search).asList()
		
		def page = attrs[page] ?: 0
		def count = attrs[count] ?: ids.size()
		
		if (page && !count) log.warning('Attribute "page" used without attribute "count"')
		
		// TODO paging on links not on resources
		ids = ids.subList(page * count, (page + 1) * count)
		
		if (!ids.isEmpty()) {
			LookUpResult res = new LookUpResult(ids.collect({ id -> get(id) }))
			
			ResourceSerializer rs = ResourceSerializerFactory.get(cf)
			rs.writeContent(res, o, cf)
		}
	}
	
	abstract protected void register(Resource res)
	
	abstract protected boolean exists(String id)
	
	abstract protected Resource get(String id)
	
	abstract protected void replace(Resource res, Resource other)
	
	abstract protected void delete(Resource res)
	
}
