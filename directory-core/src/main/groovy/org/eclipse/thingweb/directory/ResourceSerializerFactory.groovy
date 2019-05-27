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

import org.eclipse.thingweb.directory.rdf.RDFSerializer

import org.eclipse.thingweb.directory.rdf.TDSerializer

/**
 * .
 *
 * @author Victor Charpenay
 * @creation 06.08.2018
 *
 */
class ResourceSerializerFactory {
	
	/**
	 * Returns a content serializer capable of parsing and serializing resource content in the provided format.
	 * 
	 * @param cf registration type, e.g. {@code rd}, {@code td}, {@code rd} (mandatory parameter)
	 * @param params other optional parameters
	 * @return a serializer object
	 */
	static ResourceSerializer get(String rd, Map params = [:]) {
		switch (rd) {
			case 'td':
				return TDSerializer.instance
				
			case 'vocab':
				return RDFSerializer.instance

			default:
				throw new RuntimeException('No suitable resource serializer found')
		}
	}
	
}
