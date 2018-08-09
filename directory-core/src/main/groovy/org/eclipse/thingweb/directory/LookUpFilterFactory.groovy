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

import org.apache.commons.lang.NotImplementedException
import org.eclipse.thingweb.directory.rdf.RDFResourceManager
import org.eclipse.thingweb.directory.rdf.SPARQLFilter

/**
 * .
 *
 * @author Victor Charpenay
 * @creation 06.08.2018
 *
 */
class LookUpFilterFactory {

	static LookUpFilter get(String type, ResourceManager rm) {
		switch (rm) {
			case RDFResourceManager:
				switch (type) {
					case 'sem':
						def f = new SPARQLFilter()
						f.repo = rm.repo
						return f
						
					case 'res':
					case 'ep':
						// TODO
						throw new NotImplementedException()
				}
				break
				
			default:
				throw new RuntimeException('No suitable look up filter was found')
		}
	}
	
}
