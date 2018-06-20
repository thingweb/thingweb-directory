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