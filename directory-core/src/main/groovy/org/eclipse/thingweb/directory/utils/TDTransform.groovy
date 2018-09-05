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

import org.eclipse.thingweb.directory.vocabulary.JSONSCHEMA
import org.eclipse.thingweb.directory.vocabulary.TD

import groovy.json.*

/**
 * 
 * Compatibility script for JSON-LD 1.1.
 *
 * @author Victor Charpenay
 * @creation 20.06.2018
 *
 */
class TDTransform {
	
	static final TD_CONTEXT_URI = TD.NAMESPACE
	
	static final JSONSCHEMA_PROPERTIES_IRI = JSONSCHEMA.PROPERTIES.stringValue()
	
	final Object object
	
	final String base
	
	TDTransform(input) {
		assert Map.isInstance(input) : 'Object passed to TDTransform is not a JSON object'
		
		object = input
				
		if (object.'@graph') {
			object = object.'@graph'.find()
			object = withNativeTypes(object)
		}
		
		if (object.'id') base = object.'id' + '/'
		else if (object.'@id') base = object.'@id' + '/'
		else base = '/'
	}

	def asJsonLd10() {
		return asJsonLd10ForType(object, 'Thing')
	}

	def asJsonLd11() {
		return asJsonLd11ForType(object, 'Thing')
	}
	
	/**
	 * See https://github.com/jsonld-java/jsonld-java/issues/132
	 * FIXME
	 * 
	 * @param obj
	 * @return
	 */
	private withNativeTypes(obj) {
		switch (obj) {
			case Map:
				if (obj.'@value') {
					switch (obj.'@type') {
						case 'xsd:integer':
							return obj.'@value'.toInteger()
						case 'xsd:double':
							return obj.'@value'.toDouble()
						case 'xsd:boolean':
							return obj.'@value'.asBoolean()
						default:
							return obj
					}
				}
				return obj.collectEntries({ k, v -> [(k): withNativeTypes(v)] })
			case List:
				return obj.collect({ i -> withNativeTypes(i) })
			case String:
				switch (obj) {
					case 'true':
					case 'false':
						return obj.asBoolean()
					default:
						return obj
				}
			default:
				return obj
		}
	}
	
	/**
	 * Navigates through obj and returns the referenced object if found.
	 * Else, returns an empty object.
	 * 
	 * @param obj
	 * @param ref
	 * @return
	 */
	private resolve(obj, String ref) {
		def nav = { resolved, i -> 
			resolved != [:] ? resolved : resolve(i, ref)
		}
		
		switch (obj) {
			case Map:
				if (ref == obj.'@id') return obj
				return obj.values().inject([:], nav)
			case List:
				return obj.inject([:], nav)
			default:
				return [:]
		}
	}
	
	private Map asJsonLd10ForType(obj, String type) {
		switch (type) {
			case 'Thing':
				def td = obj.collectEntries({ k, v ->
					switch (k) {
						case 'properties':
							return ['properties': v.collect({ p -> asJsonLd10ForType(p, 'Property') })]
						case 'actions':
							return ['actions': v.collect({ a -> asJsonLd10ForType(a, 'Action') })]
						case 'events':
							return ['events': v.collect({ a -> asJsonLd10ForType(a, 'Event') })]
						case 'id':
							return ['@id': v]
						default:
							return [(k): (v)]
					}
				})
		
				if (!td.'@context') td.'@context' = TD_CONTEXT_URI
				
				if (!td.'@type') td.'@type' = 'Thing'
				
				// TODO
				// - @base = @id
				// - add default values
				// - context and type can be arrays
				return td
				
			case 'Property':
				def id = base + obj.key
				def p = obj.value
				if (!p.'writable') p.'writable' 
				return [
					'@id': id,
					*:asJsonLd10ForType(p, 'Schema'),
					'writable': p.'writable' ?: false,
					'observable': p.'observable' ?: false
				]
			
			case 'Action':
				def id = base + obj.key
				def a = obj.value
				def i = a.'input'
				def o = a.'output'
				if (i) a.'input' = asJsonLd10ForType(i, 'Schema')
				if (o) a.'input' = asJsonLd10ForType(o, 'Schema')
				return ['@id': id, *:a]
			
			case 'Event':
				def id = base + obj.key
				def e = obj.value
				return ['@id': id, *:e] // TODO

			case 'Schema':
				return obj.collectEntries({ k, v ->
					switch (k) {
						case 'properties':
							return [
								(JSONSCHEMA_PROPERTIES_IRI): v.collect({ s ->
									asJsonLd10ForType(s, 'ObjectSchema')
								})
							]
						case 'items':
							return ['items': asJsonLd10ForType(v, 'Schema')]
						default:
							return [(k): (v)]
					}
				})
				
			case 'ObjectSchema':
				def id = base + obj.key
				def s = obj.value
				return ['@id': id, *:s]
			
			default:
				return obj
		}
	}
	
	private Map asJsonLd11ForType(obj, String type) {
		switch (type) {
			case 'Thing':
				def td = obj
				
				td.'@context' = TD_CONTEXT_URI
				
				td = td.collectEntries({ k, v ->
					switch (k) {
						case 'properties':
							return ['properties': v.collectEntries({ p -> asJsonLd11ForType(p, 'Property')})]
						case 'actions':
							return ['actions': v.collectEntries({ a -> asJsonLd11ForType(a, 'Action')})]
						case 'events':
							return ['events': v.collectEntries({ e -> asJsonLd11ForType(e, 'Event')})]
						case '@id':
							return ['id': v]
						default:
							return [(k): (v)]
					}
				})
				
				return td
			
			case 'Property':
				def id = obj.'@id'.replace(base, '')
				def p = obj.findAll({ it.key != '@id' })
				return [(id): asJsonLd11ForType(p, 'Schema')]
			
			case 'Action':
				def id = obj.'@id'.replace(base, '')
				def a = obj.findAll({ it.key != '@id' })
				def i = a.'input'
				def o = a.'output'
				if (i) a.'input' = asJsonLd11ForType(i, 'Schema')
				if (o) a.'output' = asJsonLd11ForType(o, 'Schema')
				return [(id): a]
			
			case 'Event':
				def id = obj.'@id'.replace(base, '')
				def e = obj.findAll({ it.key != '@id' })
				return [(id): e] // TODO
			
			case 'Schema':
				if (obj instanceof String) obj = resolve(object, obj) // schema reference
				return obj.collectEntries({ k, v ->
					switch (k) {
						case JSONSCHEMA_PROPERTIES_IRI:
							return [
								'properties': v.collectEntries({ s ->
									asJsonLd11ForType(s, 'ObjectSchema')
								})
							]
						case 'items':
							return ['items': asJsonLd11ForType(v, 'Schema')]
						case '@id':
							return [:]
						default:
							return [(k): (v)]
					}
				})
				
			case 'ObjectSchema':
				def id = obj instanceof String ? obj : obj.'@id'
				id = id.replace(base, '')
				return [(id): asJsonLd11ForType(obj, 'Schema')]
			
			default:
				return obj
		}
	}

}