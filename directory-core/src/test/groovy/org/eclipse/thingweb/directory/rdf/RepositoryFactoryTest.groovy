package org.eclipse.thingweb.directory.rdf

import org.eclipse.rdf4j.repository.sail.SailRepository
import org.junit.Test

class RepositoryFactoryTest {

	@Test
	void testGetParams() {
		def repo = RepositoryFactory.get()
		
		assert SailRepository.isInstance(repo) : 'Default repository returned by factory is not of the expected type'
		
		// TODO
	}
	
}
