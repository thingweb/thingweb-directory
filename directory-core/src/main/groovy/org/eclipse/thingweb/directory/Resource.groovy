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

/**
 * Resource as specified in the IETF CoRE Resource Directory Draft. A resource
 * typically includes links to other resources (hypermedia reference, relation
 * type, content type, etc).
 *
 * @see
 *   <a href="https://tools.ietf.org/html/draft-ietf-core-resource-directory-14">
 *     CoRE Resource Directory Draft
 *   </a>
 *
 * @author Victor Charpenay
 * @creation 06.08.2018
 *
 */
interface Resource {
	
	/**
	 * Endpoint from which resource registration originates.
	 */
	String ep
	
	/**
	 * Resource lifetime, after which the resource is automatically de-registered.
	 */
	Integer lt
	
	/**
	 * Optional base URI to resolve links to other resources.
	 */
	String base
	
	/**
	 * Each resource must be uniquely identified among the resources managed by a
	 * {@link org.eclipse.thingweb.directory.ResourceManager ResourceManager} instance
	 * 
	 * @return resource identifier (also called {@code location})
	 */
	String getId()
	
	/**
	 * Merges the content of another resource with current resource. The result of a
	 * lookup is the merge of all resources matching the search criteria.
	 * 
	 * @see ResourceManager#lookUp()
	 * 
	 * @param res other resource to merge 
	 */
	void merge(Resource res)
	
}
