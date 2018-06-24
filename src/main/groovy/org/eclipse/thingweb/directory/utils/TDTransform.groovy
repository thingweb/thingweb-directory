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
	
	// TODO unify with TDServlet.TD_CONTEXT_URI
	private final TD_CONTEXT_URI = "https://w3c.github.io/wot/w3c-wot-td-context.jsonld"
	
	Object object
	
	TDTransform(input) {
		object = new JsonSlurper().parse(input)
	}

	String asJsonLd10() {
		def ctx = ["@context": ["properties": "http://www.w3.org/ns/td/schema#properties"]]
		
		def td = object.collectEntries({ k, v ->
			switch (k) {
				case "properties":
					v = v.collect({ id, p -> ["@id": id, *:ctx, *:asJsonLd10Schema(p)] })
					break
				case "actions":
					v = v.collect({ id, a ->
						def i = asJsonLd10Schema(a."input")
						def o = asJsonLd10Schema(a."output")
						["@id": id, *:ctx, *:a, "input": i, "output": o]
					})
					break
				case "events":
					// TODO
					break
			}
			[(k): (v)]
		})
		
		if (!td."@context") {
			td."@context" = TD_CONTEXT_URI
		}
		
		if (!td."@type") {
			td."@type" = "Thing"
		}
		
		td."@id" = td."id"
		td.remove("id")
		
		// TODO
		// - add default values
		// - context and type can be arrays
		
		JsonOutput.toJson(td)
	}

	String asJsonLd11() {
		def td = object
		
		if (td."@graph") {
			td = td."@graph".find()
			td."@context" = TD_CONTEXT_URI
		}
		
		td = td.collectEntries({ k, v ->
			switch (k) {
				case "properties":
					v = v.collectEntries({ p -> [(p."@id"): asJsonLd11Schema(p.findAll({ it.key != "@id" })) ] })
					break
				case "actions":
					v = v.collectEntries({ a ->
						def i = asJsonLd11Schema(a."input")
						def o = asJsonLd11Schema(a."output")
						[(a."@id"): [*:a.findAll({ !["@id", "input", "output"].contains(it) }), "input": i, "output": o]]
					})
					break
				case "events":
					// TODO
					break
			}
			[(k): (v)]
		})
		
		td."id" = td."@id"
		td.remove("@id")

		JsonOutput.toJson(td)
	}
	
	private def asJsonLd10Schema(obj) {
		if (obj instanceof Map) {
			return obj.collectEntries({ k, v ->
				switch (k) {
					case "properties":
						v = v.collect({ id, s -> ["@id": id, *:asJsonLd10Schema(s)] })
						break
					case "items":
						v = asJsonLd10Schema(v)
						break
				}
				[(k): (v)]
			})
		}
	}
	
	private def asJsonLd11Schema(obj) {
		if (obj instanceof Map) {
			return obj.collectEntries({ k, v ->
				switch (k) {
					case "http://www.w3.org/ns/td/schema#properties":
						k = "properties"
						v = v.collectEntries({ s -> [(s."@id"): asJsonLd11Schema(s.findAll({ it.key != "@id" }))] })
					case "items":
						v = asJsonLd11Schema(v)
						break
				}
				[(k): (v)]
			})
		}
	}

}