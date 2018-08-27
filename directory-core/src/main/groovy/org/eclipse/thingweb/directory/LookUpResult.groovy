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
 * 
 * Result of a lookup request encapsulating resources.
 *
 * @author Victor Charpenay
 * @creation 13.08.2018
 *
 */
class LookUpResult implements Resource {
	
	final Set<Resource> resources
	
	LookUpResult(Collection<Resource> resources) {
		this.resources = new HashSet<>(resources)
	}
	
	/**
	 * A Lookup result has no identifier itself.
	 * 
	 * @returns null
	 */
	@Override
	public String getId() {
		return null;
	}

}
