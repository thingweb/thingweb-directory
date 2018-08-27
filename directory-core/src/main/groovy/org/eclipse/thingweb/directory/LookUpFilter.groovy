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

import java.util.Set

/**
 * Common interface for the different lookup types specified by the IETF CoRE
 * Resource Directory (e.g. by endpoint or by resource attributes).
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
interface LookUpFilter {

	/**
	 * Filters resources known by a {@link ResourceManager ResourceManager} instance
	 * that match the given search query.
	 * 
	 * @param search search query (depends on lookup type)
	 * @return a set of resource identifiers (or {@code location}s)
	 */
	Set<String> filter(search)
	
}
