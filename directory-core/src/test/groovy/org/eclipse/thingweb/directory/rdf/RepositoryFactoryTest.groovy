package org.eclipse.thingweb.directory.rdf

import static groovy.test.GroovyAssert.*
import static org.eclipse.thingweb.directory.rdf.RepositoryFactory.*

import org.eclipse.rdf4j.repository.sail.SailRepository
import org.junit.Test

class RepositoryFactoryTest {

	@Test
	void testGet() {
		def repo = RepositoryFactory.get()
		
		assert SailRepository.isInstance(repo) : 'Default repository returned by factory is not of the expected type'
		
		shouldFailWithCause(RuntimeException.class, {
			RepositoryFactory.get([
				(REMOTE_SPARQL_ENDPOINT_ONLY): true,
				(SPARQL_QUERY_ENDPOINT_PROPERTY): 'tag:nonexistingendpoint'
			])
		})
	}
	
}
