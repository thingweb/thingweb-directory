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

import org.junit.Ignore
import org.junit.Test

/**
 * .
 *
 * @author Victor Charpenay
 * @creation 08.08.2018
 *
 */
class ResourceManagerTest {

	private class EmptyResource implements Resource {
		
		final String id
		
		EmptyResource(String id) {
			this.id = id
		}
		
		@Override
		public String getId() {
			return id;
		}
		
	}
	
	@Singleton
	private class EmptyResourceManager extends ResourceManager {

		private static final resources = []
		
		@Override
		public String getRegistrationType() {
			return 'vocab';
		}
		
		@Override
		public String getPreferredContentFormat() {
			return null; // none
		}

		@Override
		protected void register(Resource res) {
			resources.add(res.id)
		}
		
		@Override
		protected boolean exists(String id) {
			return resources.contains(id);
		}
		
		@Override
		protected Resource get(String id) {
			return new EmptyResource(id)
		}

		@Override
		protected void replace(Resource res, Resource other) {
			// do nothing
		}

		@Override
		protected void delete(Resource res) {
			resources.remove(res.id)
		}
		
	}
	
	@Test
	void testRegister() {
		def i = new ByteArrayInputStream('{}'.bytes)
		String id = EmptyResourceManager.instance.register(i, 'application/ld+json', [:])
		
		assert EmptyResourceManager.instance.resources.contains(id) : 'Resource has not been registered'
		
		// TODO test multiple registration
	}
	
	@Test
	@Ignore // TODO
	void testGet() {
		def i = new ByteArrayInputStream('{}'.bytes)
		String id = EmptyResourceManager.instance.register(i, 'application/ld+json', [:])
		
		def o = new ByteArrayOutputStream()
		EmptyResourceManager.instance.get(id, o, 'application/ld+json', [:])
		assert o.toByteArray().size() > 0 : 'Resource has not been properly serialized'
	}
	
	@Test
	@Ignore
	void testReplace() {
		def i = new ByteArrayInputStream('{}'.bytes)
		String id = EmptyResourceManager.instance.register(i, 'application/ld+json', [:])
		
		EmptyResourceManager.instance.replace(id, i, 'application/ld+json', [:])
		
		assert EmptyResourceManager.instance.resources.contains(id) : 'Resource has not been replaced'
	}
	
	@Test
	void testDelete() {
		def i = new ByteArrayInputStream('{}'.bytes)
		String id = EmptyResourceManager.instance.register(i, 'application/ld+json', [:])
		
		EmptyResourceManager.instance.delete(id)
		
		assert !EmptyResourceManager.instance.resources.contains(id) : 'Resource has not been deleted'
	}
	
	@Test
	@Ignore
	void testLookUp() {
		def i = new ByteArrayInputStream('{}'.bytes)
		EmptyResourceManager.instance.register(i, 'application/ld+json', [:])
		i = new ByteArrayInputStream('{}'.bytes)
		EmptyResourceManager.instance.register(i, 'application/ld+json', [:])
		
		def o = new ByteArrayOutputStream()
		def attrs = [page:0, count: 1]
		EmptyResourceManager.instance.lookUp('sem', o, 'application/ld+json', attrs, null)
		
		assert !o.toString().contains(',') : 'Paging of look up results was ignored'
	}
	
}
