package org.eclipse.thingweb.directory.rdf

import org.eclipse.thingweb.directory.ResourceManagerFactory
import org.junit.Ignore
import org.junit.Test

@Ignore
class FrameFilterTest {
	
	final RDFResourceManager m = ResourceManagerFactory.get('vocab')

	
	@Test
	void testFilter() {
		def cl = getClass().getClassLoader()
		
		def i = cl.getResourceAsStream('samples/fanTD.jsonld')
		def fan = m.register(i, 'application/ld+json', [:])
		
		i = cl.getResourceAsStream('samples/temperatureSensorTD.jsonld')
		def temp = m.register(i, 'application/ld+json', [:])
		
		def f = new FrameFilter()
		
		Set<String> filtered = f.filter('{ "@type": "http://www.w3.org/ns/td#Thing" }')
		
		assert filtered.size() == 2 : 'JSON-LD frame filter did not keep all resources'
		assert filtered.contains(fan) : 'JSON-LD frame filter did not keep fan resource'
		assert filtered.contains(temp) : 'JSON-LD frame filter did not keep temperature resource'
		
		filtered = f.filter('{ "@context": "http://www.w3.org/ns/td", "name": "myTempSensor" }')
		
		assert filtered.size() == 1 : 'JSON-LD frame filter did not filter out fan resource'
		assert filtered.contains(temp) : 'JSON-LD frame filter did not keep temperature resource'
	}
	
}
