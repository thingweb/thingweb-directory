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
		def td = asJsonLd10ForType(object, "Thing")
		JsonOutput.toJson(td)
	}

	String asJsonLd11() {
		def td = asJsonLd11ForType(object, "Thing")
		JsonOutput.toJson(td)
	}
	
	private Map asJsonLd10ForType(obj, String type) {
		switch (type) {
			case "Thing":
				def td = obj.collectEntries({ k, v ->
					switch (k) {
						case "properties":
							return ["properties": v.collect({ p -> asJsonLd10ForType(p, "Property") })]
						case "actions":
							return ["actions": v.collect({ a -> asJsonLd10ForType(a, "Action") })]
						case "events":
							return ["events": v.collect({ a -> asJsonLd10ForType(a, "Event") })]
						case "id":
							return ["@id": v]
						default:
							return [(k): (v)]
					}
				})
		
				if (!td."@context") {
					td."@context" = TD_CONTEXT_URI
				}
				
				if (!td."@type") {
					td."@type" = "Thing"
				}
				
				// TODO
				// - add default values
				// - context and type can be arrays
				return td
				
			case "Property":
				def id = obj.key
				def p = obj.value
				if (!p."writable") p."writable" 
				return [
					"@id": id,
					*:asJsonLd10ForType(p, "Schema"),
					"writable": p."writable" ?: false,
					"observable": p."observable" ?: false
				]
			
			case "Action":
				def id = obj.key
				def a = obj.value
				def i = a."input"
				def o = a."output"
				if (i) a."input" = asJsonLd10ForType(i, "Schema")
				if (o) a."input" = asJsonLd10ForType(o, "Schema")
				return ["@id": id, *:a]
			
			case "Event":
				def id = obj.key
				def e = obj.value
				return ["@id": id, *:e] // TODO

			case "Schema":
				return obj.collectEntries({ k, v ->
					switch (k) {
						case "properties":
							return [
								"http://www.w3.org/ns/td/schema#properties": v.collect({ id, s ->
									["@id": id, *:asJsonLd10ForType(s, "Schema")]
								})
							]
						case "items":
							return ["items": asJsonLd10ForType(v, "Schema")]
						default:
							return [(k): (v)]
					}
				})
			
			default:
				return obj
		}
	}
	
	private Map asJsonLd11ForType(obj, String type) {
		switch (type) {
			case "Thing":
				def td = obj
				
				if (td."@graph") {
					td = td."@graph".find()
					td."@context" = TD_CONTEXT_URI
				}
				
				td = td.collectEntries({ k, v ->
					switch (k) {
						case "properties":
							return ["properties": v.collectEntries({ p -> asJsonLd11ForType(p, "Property")})]
						case "actions":
							return ["actions": v.collectEntries({ a -> asJsonLd11ForType(a, "Action")})]
						case "events":
							return ["events": v.collectEntries({ e -> asJsonLd11ForType(e, "Event")})]
						case "@id":
							return ["id": v]
						default:
							return [(k): (v)]
					}
				})
				
				return td
			
			case "Property":
				def id = obj."@id"
				def p = obj.findAll({ it.key != "@id" })
				return [(id): asJsonLd11ForType(p, "Schema")]
			
			case "Action":
				def id = obj."@id"
				def a = obj.findAll({ it.key != "@id" })
				def i = a."input"
				def o = a."output"
				if (i) a."input" = asJsonLd11ForType(i, "Schema")
				if (o) a."output" = asJsonLd11ForType(o, "Schema")
				return [(id): a]
			
			case "Event":
				def id = obj."@id"
				def e = obj.findAll({ it.key != "@id" })
				return [(id): e] // TODO
			
			case "Schema":
				return obj.collectEntries({ k, v ->
					switch (k) {
						case "http://www.w3.org/ns/td/schema#properties":
							return [
								"properties": v.collectEntries({ s ->
									[(s."@id"): asJsonLd11ForType(s.findAll({ it.key != "@id" }), "Schema")]
								})
							]
						case "items":
							return ["items": asJsonLd11ForType(v, "Schema")]
						default:
							return [(k): (v)]
					}
				})
			
			default:
				return obj
		}
	}

}