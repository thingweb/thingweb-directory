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

import java.io.InputStream
import java.io.OutputStream

/**
 * Serializer interface for Directory {@link Resource Resource}s.
 * <p>
 * The IETF CoRE Resource Directory Draft specifies that default serialization should
 * be the CoRE Link textual format (for which JSON and CBOR serializations also exist).
 * Alternatively, RDF may also be used.
 *
 * @see
 *   <a href="https://tools.ietf.org/html/draft-ietf-core-resource-directory-14">
 *     CoRE Resource Directory Draft
 *   </a>
 *   
 * @see
 *   <a href="https://tools.ietf.org/html/rfc6690">
 *     CoRE Link Format (RFC6690)
 *   </a>
 *
 * @author Victor Charpenay
 * @creation 06.08.2018
 *
 */
interface ResourceSerializer {
	
	/**
	 * Reads the content of a resource and returns an abstract representation of it.
	 * 
	 * @param i streamed resource content
	 * @param cf resource content format, IANA-registered media type (e.g. {@code application/link-format})
	 * @return a {@link Resource Resource} object
	 */
	Resource readContent(InputStream i, String cf)

	/**
	 * Serializes the content of a {@link Resource Resource} object in the specified format.
	 * 
	 * @param res the resource to serialize
	 * @param o stream to which resource content will be written (in the format specified by {@code cf})
	 * @param cf resource content format, IANA-registered media type (e.g. {@code application/link-format})
	 */
	void writeContent(Resource res, OutputStream o, String cf)
	
}
