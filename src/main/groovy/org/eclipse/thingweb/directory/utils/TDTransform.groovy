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
package org.eclipse.thingweb.directory.utils

import groovy.json.*

/**
 * 
 *  Compatibility script for JSON-LD 1.1.
 *  TODO nested properties
 *
 * @author Victor Charpenay
 * @creation 20.06.2018
 *
 */
class TDTransform {
	
	Object object
	
	TDTransform(input) {
		object = new JsonSlurper().parse(input)
	}

	String asJsonLd10() {
		def td = object.collectEntries({ k, v ->
			switch (k) {
				case "properties":
				case "actions":
				case "events":
					v = v.collect({ kp, vp -> [ "@id": kp, *:vp ] })
					break
			}
			[(k): (v)]
		})
		
		td."@id" = td."id"
		td.remove("id")
		
		JsonOutput.toJson(td)
	}

	String asJsonLd11() {
		def td = object
		
		if (td.containsKey("@graph")) {
			td = td."@graph".find()
			td."@context" = "https://w3c.github.io/wot/w3c-wot-td-context.jsonld"
		}
		
		td = td.collectEntries({ k, v ->
			switch (k) {
				case "properties":
				case "actions":
				case "events":
					v = v.collectEntries({ vp -> [(vp."@id"): vp.findAll({ it.key != "@id" }) ] })
					break
			}
			[(k): (v)]
		})
		
		td."id" = td."@id"
		td.remove("@id")
		
		JsonOutput.toJson(td)
	}

}