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
package org.eclipse.thingweb.directory.rdf

import groovy.json.*

import java.io.InputStream
import java.io.OutputStream

import org.eclipse.thingweb.directory.Resource
import org.eclipse.thingweb.directory.ResourceSerializer
import org.eclipse.thingweb.directory.utils.TDTransform

import com.github.jsonldjava.core.JsonLdOptions
import com.github.jsonldjava.core.JsonLdProcessor

/**
 * Serializer implementation for Thing Description documents,
 * interpreted as JSON-LD 1.1 objects with normative context.
 *
 * @see
 *   <a href="http://www.w3.org/TR/wot-thing-description">
 *     W3C Thing Description Model
 *   </a>
 *
 * @author Victor Charpenay
 * @creation 06.08.2018
 *
 */
class TDSerializer extends RDFSerializer {
	
	public static final TD_FRAME = '{"@context": "' + TDTransform.TD_CONTEXT_URI + '", "@type": "Thing"}'
	
	public static final TD_CONTENT_FORMAT = 'application/td+json'
	
	private static final JSON_LD_CONTENT_FORMAT = 'application/ld+json'

	@Override
	public Resource readContent(InputStream i, String cf) {
		assert cf == TD_CONTENT_FORMAT
		
		def str = new TDTransform(i).asJsonLd10()
		i = new ByteArrayInputStream(str.bytes)
		
		return super.readContent(i, JSON_LD_CONTENT_FORMAT);
	}

	@Override
	void writeContent(Resource res, OutputStream o, String cf) {
		assert cf == TD_CONTENT_FORMAT
		
		def buf = new ByteArrayOutputStream()
		super.writeContent(res, buf, JSON_LD_CONTENT_FORMAT)
		
		// applies JSON-LD framing first
		def i = new ByteArrayInputStream(buf.toByteArray())
		def obj = new JsonSlurper().parse(i)
		def opts = new JsonLdOptions()
		// TODO use Groovy destructuring?
		opts.compactArrays = true
		opts.pruneBlankNodeIdentifiers = true
		def framed = JsonLdProcessor.frame(obj, TD_FRAME, opts)
		
		def str = new TDTransform(framed).asJsonLd10()
		o.write(str.bytes)
	}

}
